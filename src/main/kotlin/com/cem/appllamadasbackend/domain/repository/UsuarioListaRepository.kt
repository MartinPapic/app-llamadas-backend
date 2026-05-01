package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.UsuarioLista
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UsuarioListaRepository : JpaRepository<UsuarioLista, Long> {
    fun findAllByUsuarioId(usuarioId: String): List<UsuarioLista>
    fun findAllByListaId(listaId: String): List<UsuarioLista>
    fun findByUsuarioIdAndListaId(usuarioId: String, listaId: String): UsuarioLista?
}
