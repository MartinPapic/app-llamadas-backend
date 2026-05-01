package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Lista
import com.cem.appllamadasbackend.domain.model.UsuarioLista
import com.cem.appllamadasbackend.domain.repository.ListaRepository
import com.cem.appllamadasbackend.domain.repository.UsuarioListaRepository
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/listas")
class ListaController(
    private val listaRepository: ListaRepository,
    private val usuarioListaRepository: UsuarioListaRepository,
    private val usuarioRepository: UsuarioRepository
) {

    @GetMapping("/proyecto/{proyectoId}")
    fun listarPorProyecto(@PathVariable proyectoId: String): ResponseEntity<List<Lista>> =
        ResponseEntity.ok(listaRepository.findAllByProyectoId(proyectoId))

    @PostMapping
    fun crear(@RequestBody lista: Lista): ResponseEntity<Lista> {
        val id = if (lista.id.isBlank()) UUID.randomUUID().toString() else lista.id
        val nueva = lista.copy(
            id = id,
            fechaCreacion = if (lista.fechaCreacion == 0L) System.currentTimeMillis() else lista.fechaCreacion
        )
        return ResponseEntity.ok(listaRepository.save(nueva))
    }

    @DeleteMapping("/{id}")
    fun eliminar(@PathVariable id: String): ResponseEntity<Void> {
        listaRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    // --- Asignaciones de Agentes a Listas ---

    @PostMapping("/asignar")
    fun asignarAgente(@RequestBody asignacion: UsuarioLista): ResponseEntity<UsuarioLista> {
        // Verificar si ya existe
        val existente = usuarioListaRepository.findByUsuarioIdAndListaId(asignacion.usuarioId, asignacion.listaId)
        if (existente != null) {
            return ResponseEntity.ok(existente)
        }
        val nuevaAsignacion = asignacion.copy(
            fechaAsignacion = if (asignacion.fechaAsignacion == 0L) System.currentTimeMillis() else asignacion.fechaAsignacion
        )
        return ResponseEntity.ok(usuarioListaRepository.save(nuevaAsignacion))
    }

    @GetMapping("/{listaId}/agentes")
    fun agentesDeLista(@PathVariable listaId: String): ResponseEntity<List<Map<String, Any>>> {
        val asignaciones = usuarioListaRepository.findAllByListaId(listaId)
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
        usuarioListaRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/mis-listas")
    fun listarMisListas(@AuthenticationPrincipal email: String): ResponseEntity<List<Lista>> {
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(401).build()
        
        val asignaciones = usuarioListaRepository.findAllByUsuarioId(usuario.id)
        val listasIds = asignaciones.map { it.listaId }
        val listas = listaRepository.findAllById(listasIds)
        return ResponseEntity.ok(listas)
    }
}
