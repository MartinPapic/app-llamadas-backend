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
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import com.cem.appllamadasbackend.domain.repository.TipificacionRepository
import com.cem.appllamadasbackend.domain.repository.UsuarioListaRepository

@RestController
@RequestMapping("/")
class SyncController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val usuarioRepository: UsuarioRepository,
    private val tipificacionRepository: TipificacionRepository,
    private val usuarioListaRepository: UsuarioListaRepository
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
                        val tipificacionesCierre = tipificacionRepository.findAll()
                            .filter { it.cierraCaso }
                            .map { it.nombre.uppercase() }
                        
                        val huboCierreForzado = llamadasDelContacto.any { 
                            it.tipificacion != null && tipificacionesCierre.contains(it.tipificacion.uppercase()) 
                        }
                        
                        // Determinar si fue un intento válido (basado en el boolean que manda Android o backend fallback)
                        val ultimaLlamadaEsValida = ultimaLlamada?.intentoValido ?: true
                        if (ultimaLlamadaEsValida) {
                            contacto.intentosValidos += 1
                        }

                        if (huboExito) {
                            contacto.estado = EstadoContacto.CONTACTADO
                        } else if (huboCierreForzado || contacto.intentosValidos >= 5) {
                            contacto.estado = EstadoContacto.CERRADO_POR_INTENTOS
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

    // ─── POST /calls — registra una llamada individual (Sync Inmediato) ────────
    @PostMapping("/calls")
    fun registrarLlamada(@RequestBody llamada: Llamada, @AuthenticationPrincipal email: String): ResponseEntity<Map<String, String>> {
        val payload = SyncPayload(
            llamadas = listOf(llamada),
            contactosActualizados = emptyList()
        )
        val response = syncData(payload, email)
        return if (response.statusCode.is2xxSuccessful) {
            ResponseEntity.ok(mapOf("mensaje" to "Llamada registrada exitosamente"))
        } else {
            ResponseEntity.status(response.statusCode).body(mapOf("error" to "Error al registrar llamada"))
        }
    }

    // ─── GET /contacts — lista de contactos (con filtro opcional) ─────────
    @GetMapping("/contacts")
    fun getContactos(
        @RequestParam(required = false) estado: String?,
        @RequestParam(required = false) proyectoId: String?,
        @AuthenticationPrincipal email: String
    ): ResponseEntity<List<Contacto>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()

        val ahora = System.currentTimeMillis()
        val diezMinutos = 10 * 60 * 1000

        val misListas = usuarioListaRepository.findAllByUsuarioId(usuario.id).map { it.listaId }.toSet()

        val todos = contactoRepository.findAll().filter { contacto ->
            val bloqueadoPorOtro = contacto.bloqueadoPor != null && 
                                  contacto.bloqueadoPor != usuario.id && 
                                  contacto.bloqueadoPor != usuario.email &&
                                  (ahora - (contacto.fechaBloqueo ?: 0L)) < diezMinutos
            !bloqueadoPorOtro && (contacto.listaId in misListas)
        }
        
        val resultado = todos
            .let { if (proyectoId != null) it.filter { c -> c.proyectoId == proyectoId } else it }
            .let { if (estado != null) it.filter { c -> c.estado.name.equals(estado, ignoreCase = true) } else it }

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

        val misListas = usuarioListaRepository.findAllByUsuarioId(usuario.id).map { it.listaId }.toSet()

        val pendientes = contactoRepository.findAll().filter { contacto ->
            val bloqueadoPorOtro = contacto.bloqueadoPor != null && 
                                  contacto.bloqueadoPor != usuario.id && 
                                  contacto.bloqueadoPor != usuario.email &&
                                  (ahora - (contacto.fechaBloqueo ?: 0L)) < diezMinutos
                                  
            contacto.estado != EstadoContacto.DESISTIDO && 
            contacto.estado != EstadoContacto.CONTACTADO &&
            contacto.estado != EstadoContacto.CERRADO &&
            contacto.estado != EstadoContacto.CERRADO_POR_INTENTOS &&
            !bloqueadoPorOtro && (contacto.listaId in misListas)
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
