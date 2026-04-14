package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Llamada
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

import org.springframework.data.jpa.repository.Query

interface ResultadoDistribucion {
    val resultado: com.cem.appllamadasbackend.domain.model.ResultadoLlamada?
    val cantidad: Long
}

@Repository
interface LlamadaRepository : JpaRepository<Llamada, String> {

    @Query("SELECT l.resultado AS resultado, COUNT(l) AS cantidad FROM Llamada l " +
           "WHERE l.fechaInicio >= :inicioDia GROUP BY l.resultado")
    fun countResultadosDesde(inicioDia: Long): List<ResultadoDistribucion>

    @Query("SELECT COUNT(l) FROM Llamada l WHERE l.fechaInicio >= :inicioDia")
    fun countLlamadasDesde(inicioDia: Long): Long
}
