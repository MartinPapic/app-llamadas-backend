package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Contacto
import com.cem.appllamadasbackend.domain.model.Llamada
import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import com.cem.appllamadasbackend.domain.model.ResultadoLlamada
import com.cem.appllamadasbackend.domain.model.EstadoContacto

import org.springframework.web.bind.annotation.*
import com.cem.appllamadasbackend.domain.repository.EncuestaRepository
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository

@RestController
@RequestMapping("/")
class SyncController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val usuarioRepository: UsuarioRepository,
    private val encuestaRepository: EncuestaRepository
) {

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    data class SyncPayload(
        val llamadas: List<Llamada>,
        val contactosActualizados: List<Contacto>
    )

    data class SyncResponse(
        val success: Boolean,
        val message: String,
        val synchronizedIds: List<String>
    )

    data class EncuestaDto(
        val id: String,
        val contactoId: String,
        val url: String,
        val estado: String,
        val fecha: Long
    )

    data class EncuestaSyncRequest(
        val encuestas: List<EncuestaDto>
    )

    // ─── POST /api/contacts/{id}/lock — Bloqueo preventivo pro-Pool ─────────────
    @PostMapping("/contacts/{id}/lock")
    @Transactional
    fun lockContacto(
        @PathVariable id: String,
        @AuthenticationPrincipal email: String
    ): ResponseEntity<Map<String, Any>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()

        val contactoOpt = contactoRepository.findById(id)
        if (!contactoOpt.isPresent) return ResponseEntity.notFound().build()
        val contacto = contactoOpt.get()

        val ahora = System.currentTimeMillis()
        val diezMinutos = 10 * 60 * 1000

        // Verificar si está bloqueado por otro con tiempo vigente
        val bloqueadoPorOtro = contacto.bloqueadoPor != null && 
                              contacto.bloqueadoPor != usuario.id && 
                              contacto.bloqueadoPor != usuario.email &&
                              (ahora - (contacto.fechaBloqueo ?: 0L)) < diezMinutos

        if (bloqueadoPorOtro) {
             return ResponseEntity.status(409).body(mapOf("error" to "Contacto ocupado por otro agente"))
        }

        // Adquirir bloqueo
        contacto.bloqueadoPor = usuario.id
        contacto.fechaBloqueo = ahora
        contactoRepository.save(contacto)

        return ResponseEntity.ok(mapOf("success" to true, "expiresIn" to diezMinutos))
    }

    @PostMapping("/sync")
    @Transactional
    fun syncData(
        @RequestBody payload: SyncPayload,
        @AuthenticationPrincipal email: String
    ): ResponseEntity<SyncResponse> {
        return try {
            // 1. Persistir llamadas primero
            if (payload.llamadas.isNotEmpty()) {
                llamadaRepository.saveAll(payload.llamadas)
            }

            // 2. Control Server-Side de estados (Authoritative State)
            if (payload.llamadas.isNotEmpty()) {
                payload.llamadas.groupBy { it.contactoId }.forEach { (contactoId, llamadasDelContacto) ->
                    val contactoOpt = contactoRepository.findById(contactoId)
                    if (contactoOpt.isPresent) {
                        val contacto = contactoOpt.get()
                        
                        // Encontrar la última llamada (por fecha o simplemente la última del batch)
                        val ultimaLlamada = llamadasDelContacto.maxByOrNull { it.fechaInicio }
                        
                        val huboExito = llamadasDelContacto.any { it.resultado == ResultadoLlamada.CONTACTADO_EFECTIVO }
                        
                        // Lista de tipificaciones que cierran el caso
                        val tipificacionesCierre = listOf(
                            "NUMERO_NO_CORRESPONDE", 
                            "NUMERO_NO_EXISTE", 
                            "DESISTIO_PROYECTO",
                            "RECHAZO_EXPLICITO",
                            "NO_CONTACTAR_NUEVAMENTE"
                        )
                        
                        val huboCierreForzado = llamadasDelContacto.any { 
                            it.tipificacion != null && tipificacionesCierre.contains(it.tipificacion.uppercase()) 
                        }
                        
                        if (huboExito) {
                            contacto.estado = EstadoContacto.CONTACTADO
                        } else if (huboCierreForzado || contacto.intentos >= 5) {
                            contacto.estado = EstadoContacto.DESISTIDO
                        } else if (contacto.estado == EstadoContacto.PENDIENTE) {
                            contacto.estado = EstadoContacto.EN_GESTION
                        }

                        // Actualizar info de última gestión
                        if (ultimaLlamada != null) {
                            contacto.ultimaTipificacion = ultimaLlamada.tipificacion
                            contacto.ultimaObservacion = ultimaLlamada.observacion
                        }

                        // Liberar bloqueo tras sync exitoso
                        contacto.bloqueadoPor = null
                        contacto.fechaBloqueo = null
                        
                        contactoRepository.save(contacto)
                    }
                }
            }

            // 3. Persistir intentos (si el cliente mandó contactos actualizados sin llamadas)
            if (payload.contactosActualizados.isNotEmpty()) {
                val ids = payload.contactosActualizados.map { it.id }
                val mapExistentes = contactoRepository.findAllById(ids).associateBy { it.id }
                
                val contactosToSave = payload.contactosActualizados.mapNotNull { dto ->
                     val existente = mapExistentes[dto.id]
                     if (existente != null && existente.estado != EstadoContacto.CONTACTADO && existente.estado != EstadoContacto.DESISTIDO) {
                         existente.intentos = dto.intentos
                         // Asegurarse de no sobrescribir el estado calculado
                         existente
                     } else null
                }
                contactoRepository.saveAll(contactosToSave)
            }

            val ids = payload.llamadas.map { it.id }
            ResponseEntity.ok(SyncResponse(true, "Sync exitoso: ${ids.size} llamadas procesadas", ids))
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.internalServerError()
                .body(SyncResponse(false, e.message ?: "Error de sincronización", emptyList()))
        }
    }

    // ─── POST /api/encuestas — guarda encuesta individual o batch ──────────────
    @PostMapping("/encuestas")
    fun saveEncuestas(@RequestBody request: EncuestaSyncRequest): ResponseEntity<Map<String, String>> {
        val entidades = request.encuestas.map { dto ->
            com.cem.appllamadasbackend.domain.model.Encuesta(
                id = dto.id,
                contactoId = dto.contactoId,
                url = dto.url,
                estado = dto.estado,
                fecha = dto.fecha
            )
        }
        encuestaRepository.saveAll(entidades)
        return ResponseEntity.ok(mapOf("mensaje" to "${entidades.size} encuestas sincronizadas"))
    }

    // ─── GET /contacts — lista de contactos (con filtro opcional) ─────────
    @GetMapping("/contacts")
    fun getContactos(
        @RequestParam(required = false) estado: String?,
        @AuthenticationPrincipal email: String
    ): ResponseEntity<List<Contacto>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()

        val ahora = System.currentTimeMillis()
        val diezMinutos = 10 * 60 * 1000

        val todos = contactoRepository.findAll().filter { contacto ->
            val bloqueadoPorOtro = contacto.bloqueadoPor != null && 
                                  contacto.bloqueadoPor != usuario.id && 
                                  contacto.bloqueadoPor != usuario.email &&
                                  (ahora - (contacto.fechaBloqueo ?: 0L)) < diezMinutos
            !bloqueadoPorOtro
        }
        
        val resultado = if (estado != null) {
            todos.filter { it.estado.name.equals(estado, ignoreCase = true) }
        } else {
            todos
        }
        return ResponseEntity.ok(resultado)
    }

    // ─── GET /contacts/{id} — detalle de contacto ───────────────────────────
    @GetMapping("/contacts/{id}")
    fun getContactoById(@PathVariable id: String): ResponseEntity<Contacto> {
        return contactoRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    // ─── GET /contactos/pendientes — alias para compatibilidad ────────────
    @GetMapping("/contactos/pendientes")
    fun getContactosPendientes(@AuthenticationPrincipal email: String): ResponseEntity<List<Contacto>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()

        val ahora = System.currentTimeMillis()
        val diezMinutos = 10 * 60 * 1000

        val pendientes = contactoRepository.findAll().filter { contacto ->
            val bloqueadoPorOtro = contacto.bloqueadoPor != null && 
                                  contacto.bloqueadoPor != usuario.id && 
                                  contacto.bloqueadoPor != usuario.email &&
                                  (ahora - (contacto.fechaBloqueo ?: 0L)) < diezMinutos
                                  
            contacto.estado != EstadoContacto.DESISTIDO && 
            contacto.estado != EstadoContacto.CONTACTADO &&
            !bloqueadoPorOtro
        }
        return ResponseEntity.ok(pendientes)
    }

    // ─── GET /calls — historial de llamadas ────────────────────────────
    @GetMapping("/calls")
    fun getLlamadas(
        @RequestParam(required = false) contactoId: String?
    ): ResponseEntity<List<Llamada>> {
        val todas = llamadaRepository.findAll()
        val resultado = if (contactoId != null) {
            todas.filter { it.contactoId == contactoId }
        } else {
            todas
        }
        return ResponseEntity.ok(resultado)
    }
}
