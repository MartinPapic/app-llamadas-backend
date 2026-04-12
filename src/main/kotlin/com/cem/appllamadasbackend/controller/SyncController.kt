package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Contacto
import com.cem.appllamadasbackend.domain.model.Llamada
import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class SyncController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository
) {

    // Helper classes to accept batch sync
    data class SyncPayload(
        val llamadas: List<Llamada>,
        val contactosActualizados: List<Contacto>
    )
    
    data class SyncResponse(
        val success: Boolean,
        val message: String,
        val synchronizedIds: List<String>
    )

    @PostMapping("/sync")
    fun syncData(@RequestBody payload: SyncPayload): ResponseEntity<SyncResponse> {
        try {
            // Save or Update Contactos and Llamadas
            contactoRepository.saveAll(payload.contactosActualizados)
            llamadaRepository.saveAll(payload.llamadas)
            
            val ids = payload.llamadas.map { it.id }
            
            return ResponseEntity.ok(SyncResponse(true, "Sync successful", ids))
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.internalServerError().body(SyncResponse(false, e.message ?: "Unknown error", emptyList()))
        }
    }

    @GetMapping("/contactos/pendientes")
    fun getContactosPendientes(): ResponseEntity<List<Contacto>> {
        // En una app real esto tendría un filtro avanzado
        return ResponseEntity.ok(contactoRepository.findAll())
    }
}
