package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.UsuarioProyecto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UsuarioProyectoRepository : JpaRepository<UsuarioProyecto, Long> {
    fun findAllByUsuarioId(usuarioId: String): List<UsuarioProyecto>
    fun findAllByProyectoId(proyectoId: String): List<UsuarioProyecto>
}
