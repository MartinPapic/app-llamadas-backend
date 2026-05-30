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
        @RequestParam(required = false) proyectoId: String?
    ): ResponseEntity<Page<Contacto>> {
        val pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending())
        return if (proyectoId.isNullOrBlank()) {
            ResponseEntity.ok(contactoRepository.findAll(pageable))
        } else {
            ResponseEntity.ok(contactoRepository.findByProyectoId(proyectoId, pageable))
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
        // Enviar todos (ADMIN y AGENTE) para que el CSV pueda emparejar contactos a admins testeando.
        val agentes = usuarioRepository.findAll()
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
