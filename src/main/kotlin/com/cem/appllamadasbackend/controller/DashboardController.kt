package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Contacto
import com.cem.appllamadasbackend.domain.model.Llamada
import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class MetricasResponse(
    val totalContactos: Long,
    val totalLlamadas: Long,
    val totalContestan: Long,
    val totalNoContestan: Long,
    val duracionPromedio: Double,
    val tasaContacto: Double
)

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository
) {

    @GetMapping("/metricas")
    fun getMetricas(): ResponseEntity<MetricasResponse> {
        val totalContactos = contactoRepository.count()
        val todasLlamadas  = llamadaRepository.findAll()
        val totalLlamadas  = todasLlamadas.size.toLong()
        val contestan      = todasLlamadas.count { it.resultado == "CONTESTA" }.toLong()
        val noContestan    = todasLlamadas.count { it.resultado == "NO_CONTESTA" }.toLong()
        val durPromedio    = todasLlamadas.mapNotNull { it.duracion }.let {
            if (it.isEmpty()) 0.0 else it.average()
        }
        val tasa = if (totalLlamadas > 0) (contestan.toDouble() / totalLlamadas) * 100 else 0.0

        return ResponseEntity.ok(MetricasResponse(
            totalContactos  = totalContactos,
            totalLlamadas   = totalLlamadas,
            totalContestan  = contestan,
            totalNoContestan = noContestan,
            duracionPromedio = durPromedio,
            tasaContacto    = tasa
        ))
    }

    @GetMapping("/llamadas")
    fun getLlamadas(): ResponseEntity<List<Llamada>> =
        ResponseEntity.ok(llamadaRepository.findAll())

    @GetMapping("/contactos")
    fun getContactos(): ResponseEntity<List<Contacto>> =
        ResponseEntity.ok(contactoRepository.findAll())

    @PostMapping("/contactos")
    fun crearContacto(@RequestBody contacto: Contacto): ResponseEntity<Contacto> =
        ResponseEntity.ok(contactoRepository.save(contacto))
}
