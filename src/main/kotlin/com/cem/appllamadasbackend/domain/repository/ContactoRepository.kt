package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Contacto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import com.cem.appllamadasbackend.domain.model.EstadoContacto

interface EstadoDistribucion {
    val estado: com.cem.appllamadasbackend.domain.model.EstadoContacto
    val cantidad: Long
}

@Repository
interface ContactoRepository : JpaRepository<Contacto, String> {

    @Query("SELECT c.estado AS estado, COUNT(c) AS cantidad FROM Contacto c GROUP BY c.estado")
    fun countByEstado(): List<EstadoDistribucion>

    fun findByProyectoId(proyectoId: String, pageable: Pageable): Page<Contacto>
    fun findByEstado(estado: EstadoContacto, pageable: Pageable): Page<Contacto>
    fun findByProyectoIdAndEstado(proyectoId: String, estado: EstadoContacto, pageable: Pageable): Page<Contacto>

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE Contacto c 
        SET c.intentos = 0, c.intentosValidos = 0, c.estado = 'PENDIENTE', c.bloqueadoPor = null, c.fechaBloqueo = null 
        WHERE (:proyectoId IS NULL OR c.proyectoId = :proyectoId) 
          AND (:estado IS NULL OR c.estado = :estado)
          AND (c.estado IN ('CERRADO', 'DESISTIDO', 'CERRADO_POR_INTENTOS') OR c.intentos >= 5 OR c.intentosValidos >= 5)
    """)
    fun bulkUnlockContactos(
        @org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?, 
        @org.springframework.data.repository.query.Param("estado") estado: EstadoContacto?
    ): Int
}
