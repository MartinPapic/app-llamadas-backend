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

import com.cem.appllamadasbackend.domain.repository.ListaRepository
import com.cem.appllamadasbackend.domain.repository.UsuarioListaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

@RestController
@RequestMapping("/projects")
class ProyectoController(
    private val proyectoRepository: ProyectoRepository,
    private val usuarioProyectoRepository: UsuarioProyectoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val listaRepository: ListaRepository,
    private val usuarioListaRepository: UsuarioListaRepository,
    private val jdbcTemplate: JdbcTemplate
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
    @Transactional
    fun eliminar(@PathVariable id: String): ResponseEntity<Void> {
        // 1. Eliminar llamadas relacionadas (por proyecto_id, lista_id o contacto_id)
        jdbcTemplate.update("""
            DELETE FROM llamada 
            WHERE proyecto_id = ? 
               OR lista_id IN (SELECT id FROM lista WHERE proyecto_id = ?) 
               OR contacto_id IN (SELECT id FROM contacto WHERE proyecto_id = ? OR lista_id IN (SELECT id FROM lista WHERE proyecto_id = ?))
        """, id, id, id, id)

        // 2. Eliminar encuestas de contactos de este proyecto o sus listas
        jdbcTemplate.update("""
            DELETE FROM encuesta 
            WHERE contacto_id IN (SELECT id FROM contacto WHERE proyecto_id = ? OR lista_id IN (SELECT id FROM lista WHERE proyecto_id = ?))
        """, id, id)

        // 3. Eliminar asignaciones de agentes a listas de este proyecto
        jdbcTemplate.update("""
            DELETE FROM usuario_lista 
            WHERE lista_id IN (SELECT id FROM lista WHERE proyecto_id = ?)
        """, id)

        // 4. Eliminar asignaciones de agentes a este proyecto
        jdbcTemplate.update("DELETE FROM usuario_proyecto WHERE proyecto_id = ?", id)

        // 5. Eliminar contactos de este proyecto o de sus listas
        jdbcTemplate.update("""
            DELETE FROM contacto 
            WHERE proyecto_id = ? 
               OR lista_id IN (SELECT id FROM lista WHERE proyecto_id = ?)
        """, id, id)

        // 6. Eliminar listas del proyecto
        jdbcTemplate.update("DELETE FROM lista WHERE proyecto_id = ?", id)

        // 7. Eliminar el proyecto principal
        proyectoRepository.deleteById(id)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/agente")
    fun listarParaAgente(@AuthenticationPrincipal email: String): ResponseEntity<List<Proyecto>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()
        
        // Proyectos asignados directamente (legacy)
        val asignacionesDirectas = usuarioProyectoRepository.findAllByUsuarioId(usuario.id).map { it.proyectoId }
        
        // Proyectos derivados de las listas asignadas
        val listasAsignadas = usuarioListaRepository.findAllByUsuarioId(usuario.id).map { it.listaId }
        val proyectosDeListas = listaRepository.findAllById(listasAsignadas).map { it.proyectoId }
        
        val proyectoIds = (asignacionesDirectas + proyectosDeListas).distinct()
        val proyectos = proyectoRepository.findAllById(proyectoIds)
        
        return ResponseEntity.ok(proyectos)
    }

    @PostMapping("/asignar")
    fun asignarAgente(@RequestBody asignacion: UsuarioProyecto): ResponseEntity<UsuarioProyecto> {
        return ResponseEntity.ok(usuarioProyectoRepository.save(asignacion))
    }

    @GetMapping("/{proyectoId}/agentes")
    fun agentesDeProyecto(@PathVariable proyectoId: String): ResponseEntity<List<Map<String, Any>>> {
        val asignaciones = usuarioProyectoRepository.findAllByProyectoId(proyectoId)
        val resultado = asignaciones.mapNotNull { asig ->
            usuarioRepository.findById(asig.usuarioId).orElse(null)?.let { u ->
                mapOf(
                    "asignacionId" to (asig.id ?: 0L),
                    "id" to u.id,
                    "nombre" to u.nombre,
                    "email" to u.email
                )
            }
        }
        return ResponseEntity.ok(resultado)
    }

    @DeleteMapping("/asignaciones/{id}")
    fun desasignarAgente(@PathVariable id: Long): ResponseEntity<Void> {
        usuarioProyectoRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
