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

    fun countByListaId(listaId: String): Long
    fun countByListaIdAndEstado(listaId: String, estado: EstadoContacto): Long
    fun countByListaIdAndAgenteId(listaId: String, agenteId: String): Long
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    fun deleteByListaId(listaId: String)


    fun findByProyectoId(proyectoId: String, pageable: Pageable): Page<Contacto>
    fun findByEstado(estado: EstadoContacto, pageable: Pageable): Page<Contacto>
    fun findByProyectoIdAndEstado(proyectoId: String, estado: EstadoContacto, pageable: Pageable): Page<Contacto>

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE Contacto c 
        SET c.intentos = 0, c.intentosValidos = 0, c.estado = 'PENDIENTE', c.bloqueadoPor = null, c.fechaBloqueo = null, c.agenteId = null 
        WHERE (:proyectoId IS NULL OR c.proyectoId = :proyectoId) 
          AND (:estado IS NULL OR c.estado = :estado)
          AND (c.estado IN ('CERRADO', 'DESISTIDO', 'CERRADO_POR_INTENTOS') OR c.intentos >= 5 OR c.intentosValidos >= 5)
    """)
    fun bulkUnlockContactos(
        @org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?, 
        @org.springframework.data.repository.query.Param("estado") estado: EstadoContacto?
    ): Int

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
        UPDATE contacto 
        SET 
            estado = 'PENDIENTE', 
            agente_id = NULL, 
            bloqueado_por = NULL, 
            fecha_bloqueo = NULL,
            ultima_observacion = CASE 
                WHEN ultima_tipificacion ILIKE 'llamar m%s tarde' 
                     AND (EXTRACT(EPOCH FROM NOW()) * 1000 - fecha_ultima_gestion) > 604800000 
                THEN NULL 
                ELSE ultima_observacion 
            END,
            ultima_tipificacion = CASE 
                WHEN ultima_tipificacion ILIKE 'llamar m%s tarde' 
                     AND (EXTRACT(EPOCH FROM NOW()) * 1000 - fecha_ultima_gestion) > 604800000 
                THEN NULL 
                ELSE ultima_tipificacion 
            END
        WHERE estado = 'EN_GESTION' 
          AND (
              (ultima_tipificacion NOT ILIKE 'llamar m%s tarde' OR ultima_tipificacion IS NULL)
              OR 
              (ultima_tipificacion ILIKE 'llamar m%s tarde' 
               AND (EXTRACT(EPOCH FROM NOW()) * 1000 - fecha_ultima_gestion) > 604800000)
          )
    """, nativeQuery = true)
    fun unlockDailyContacts(): Int
}
