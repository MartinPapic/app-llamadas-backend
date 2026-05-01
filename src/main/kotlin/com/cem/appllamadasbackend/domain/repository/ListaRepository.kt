package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Lista
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ListaRepository : JpaRepository<Lista, String> {
    fun findAllByProyectoId(proyectoId: String): List<Lista>
}
