package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Contacto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

import org.springframework.data.jpa.repository.Query

interface EstadoDistribucion {
    val estado: com.cem.appllamadasbackend.domain.model.EstadoContacto
    val cantidad: Long
}

@Repository
interface ContactoRepository : JpaRepository<Contacto, String> {

    @Query("SELECT c.estado AS estado, COUNT(c) AS cantidad FROM Contacto c GROUP BY c.estado")
    fun countByEstado(): List<EstadoDistribucion>
}
