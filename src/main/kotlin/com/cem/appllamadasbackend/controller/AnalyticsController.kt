package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/analytics")
class AnalyticsController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository
) {

    @GetMapping("/realtime")
    fun getRealtimeMetrics(@RequestParam(required = false) fecha: String?): ResponseEntity<Map<String, Any>> {
        val date = fecha?.let { try { LocalDate.parse(it) } catch (e: Exception) { LocalDate.now() } } ?: LocalDate.now()
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val llamadasEmitidasHoy = llamadaRepository.countLlamadasDesde(startOfDay)
        val resultados = llamadaRepository.countResultadosDesde(startOfDay)
        val distribucion = resultados.associate { (it.resultado?.name ?: "UNKNOWN") to it.cantidad }
        
        val contestan = distribucion["CONTESTA"] ?: 0L
        val tasaContactabilidad = if (llamadasEmitidasHoy > 0) (contestan.toDouble() / llamadasEmitidasHoy) * 100 else 0.0

        val response = mapOf(
            "llamadasEmitidasHoy" to llamadasEmitidasHoy,
            "tasaContactabilidadDiaria" to tasaContactabilidad,
            "distribucionResultados" to distribucion
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/funnel")
    fun getFunnel(): ResponseEntity<Map<String, Any>> {
        val totalBase = contactoRepository.count()
        val distribucionDb = contactoRepository.countByEstado()
        val estadosMap = distribucionDb.associate { it.estado.name to it.cantidad }

        val response = mapOf(
            "totalBase" to totalBase,
            "estados" to estadosMap
        )

        return ResponseEntity.ok(response)
    }
}
