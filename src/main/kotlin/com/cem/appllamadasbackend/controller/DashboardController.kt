package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Contacto
import com.cem.appllamadasbackend.domain.model.Llamada
import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import com.cem.appllamadasbackend.domain.model.Usuario
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.cem.appllamadasbackend.domain.model.ResultadoLlamada
import com.cem.appllamadasbackend.domain.model.RolUsuario
import com.cem.appllamadasbackend.domain.model.EstadoContacto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort



@RestController
@RequestMapping("/")
class DashboardController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val usuarioRepository: UsuarioRepository
) {



    @GetMapping("/admin/calls")
    fun getLlamadas(): ResponseEntity<List<Llamada>> =
        ResponseEntity.ok(llamadaRepository.findAll())

    @GetMapping("/admin/contacts")
    fun getContactos(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(required = false) proyectoId: String?,
        @RequestParam(required = false) estado: String?
    ): ResponseEntity<Page<Contacto>> {
        val pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending())
        val enumEstado = estado?.let { EstadoContacto.fromString(it) }

        return if (!proyectoId.isNullOrBlank() && enumEstado != null) {
            ResponseEntity.ok(contactoRepository.findByProyectoIdAndEstado(proyectoId, enumEstado, pageable))
        } else if (!proyectoId.isNullOrBlank()) {
            ResponseEntity.ok(contactoRepository.findByProyectoId(proyectoId, pageable))
        } else if (enumEstado != null) {
            ResponseEntity.ok(contactoRepository.findByEstado(enumEstado, pageable))
        } else {
            ResponseEntity.ok(contactoRepository.findAll(pageable))
        }
    }

    @PostMapping("/admin/contacts")
    fun crearContacto(@RequestBody contacto: Contacto): ResponseEntity<Contacto> =
        ResponseEntity.ok(contactoRepository.save(contacto))

    @PostMapping("/admin/contacts/upload")
    fun uploadContactos(
        @RequestBody contactos: List<Contacto>,
        @RequestParam(required = false) proyectoId: String?,
        @RequestParam(required = false) listaId: String?
    ): ResponseEntity<Map<String, Any>> {
        val contactosAProcesar = contactos.map { c ->
            c.copy(
                proyectoId = proyectoId ?: c.proyectoId,
                listaId = listaId ?: c.listaId
            )
        }
        val saved = contactoRepository.saveAll(contactosAProcesar)
        return ResponseEntity.ok(mapOf("mensaje" to "Contactos importados exitosamente", "cantidad" to saved.size))
    }

    @GetMapping("/admin/agents")
    fun getAgentes(): ResponseEntity<List<Map<String, String>>> {
        // Solo devolver agentes activos para selectores de asignación
        val agentes = usuarioRepository.findByActivoTrue()
        val respuesta = agentes.map {
            mapOf(
                "id" to it.id,
                "nombre" to it.nombre,
                "email" to it.email
            )
        }
        return ResponseEntity.ok(respuesta)
    }

}
