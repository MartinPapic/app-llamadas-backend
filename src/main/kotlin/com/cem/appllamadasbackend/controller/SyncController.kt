package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Contacto
import com.cem.appllamadasbackend.domain.model.Llamada
import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository

@RestController
@RequestMapping("/api")
class SyncController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val usuarioRepository: UsuarioRepository
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

    // ─── POST /api/calls — registrar llamada individual ────────────────────────
    @PostMapping("/calls")
    fun registrarLlamada(
        @RequestBody llamada: Llamada,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val saved = llamadaRepository.save(llamada)
            ResponseEntity.ok(mapOf("id" to saved.id, "message" to "Llamada registrada"))
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "Error al registrar llamada")))
        }
    }

    // ─── POST /api/sync — sincronización batch ────────────────────────────────
    @PostMapping("/sync")
    fun syncData(
        @RequestBody payload: SyncPayload,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<SyncResponse> {
        return try {
            // Persistir contactos actualizados
            if (payload.contactosActualizados.isNotEmpty()) {
                contactoRepository.saveAll(payload.contactosActualizados)
            }
            // Persistir llamadas
            if (payload.llamadas.isNotEmpty()) {
                llamadaRepository.saveAll(payload.llamadas)
            }

            val ids = payload.llamadas.map { it.id }
            ResponseEntity.ok(SyncResponse(true, "Sync exitoso: ${ids.size} llamadas", ids))
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.internalServerError()
                .body(SyncResponse(false, e.message ?: "Error de sincronización", emptyList()))
        }
    }

    // ─── GET /api/contactos — lista de contactos (con filtro opcional) ─────────
    @GetMapping("/contactos")
    fun getContactos(
        @RequestParam(required = false) estado: String?,
        @AuthenticationPrincipal email: String
    ): ResponseEntity<List<Contacto>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()

        val todos = contactoRepository.findAll().filter { it.agenteId == usuario.id }
        val resultado = if (estado != null) {
            todos.filter { it.estado.equals(estado, ignoreCase = true) }
        } else {
            todos
        }
        return ResponseEntity.ok(resultado)
    }

    // ─── GET /api/contactos/pendientes — alias para compatibilidad ────────────
    @GetMapping("/contactos/pendientes")
    fun getContactosPendientes(@AuthenticationPrincipal email: String): ResponseEntity<List<Contacto>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()

        val pendientes = contactoRepository.findAll().filter {
            it.agenteId == usuario.id && it.estado != "desistido" && it.estado != "contactado"
        }
        return ResponseEntity.ok(pendientes)
    }

    // ─── GET /api/llamadas — historial de llamadas ────────────────────────────
    @GetMapping("/llamadas")
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
