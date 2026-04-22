package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Proyecto
import com.cem.appllamadasbackend.domain.model.UsuarioProyecto
import com.cem.appllamadasbackend.domain.repository.ProyectoRepository
import com.cem.appllamadasbackend.domain.repository.UsuarioProyectoRepository
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/proyectos")
class ProyectoController(
    private val proyectoRepository: ProyectoRepository,
    private val usuarioProyectoRepository: UsuarioProyectoRepository,
    private val usuarioRepository: UsuarioRepository
) {

    @GetMapping
    fun listarTodos(): ResponseEntity<List<Proyecto>> =
        ResponseEntity.ok(proyectoRepository.findAll())

    @PostMapping
    fun crear(@RequestBody proyecto: Proyecto): ResponseEntity<Proyecto> {
        val id = if (proyecto.id.isBlank()) UUID.randomUUID().toString() else proyecto.id
        val nuevo = proyecto.copy(id = id)
        return ResponseEntity.ok(proyectoRepository.save(nuevo))
    }

    @DeleteMapping("/{id}")
    fun eliminar(@PathVariable id: String): ResponseEntity<Void> {
        proyectoRepository.deleteById(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/agente")
    fun listarParaAgente(@AuthenticationPrincipal email: String): ResponseEntity<List<Proyecto>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()
        
        val asignaciones = usuarioProyectoRepository.findAllByUsuarioId(usuario.id)
        val proyectoIds = asignaciones.map { it.proyectoId }
        val proyectos = proyectoRepository.findAllById(proyectoIds)
        
        return ResponseEntity.ok(proyectos)
    }

    @PostMapping("/asignar")
    fun asignarAgente(@RequestBody asignacion: UsuarioProyecto): ResponseEntity<UsuarioProyecto> {
        return ResponseEntity.ok(usuarioProyectoRepository.save(asignacion))
    }
}
