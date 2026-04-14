package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Contacto
import com.cem.appllamadasbackend.domain.model.Llamada
import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import com.cem.appllamadasbackend.domain.model.Encuesta
import com.cem.appllamadasbackend.domain.repository.EncuestaRepository
import com.cem.appllamadasbackend.domain.model.Usuario
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
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
@RequestMapping("/")
class DashboardController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val usuarioRepository: UsuarioRepository,
    private val encuestaRepository: EncuestaRepository
) {

    @GetMapping("/metrics")
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

    @GetMapping("/admin/calls")
    fun getLlamadas(): ResponseEntity<List<Llamada>> =
        ResponseEntity.ok(llamadaRepository.findAll())

    @GetMapping("/admin/contacts")
    fun getContactos(): ResponseEntity<List<Contacto>> =
        ResponseEntity.ok(contactoRepository.findAll())

    @PostMapping("/admin/contacts")
    fun crearContacto(@RequestBody contacto: Contacto): ResponseEntity<Contacto> =
        ResponseEntity.ok(contactoRepository.save(contacto))

    @PostMapping("/admin/contacts/upload")
    fun uploadContactos(@RequestBody contactos: List<Contacto>): ResponseEntity<Map<String, Any>> {
        val saved = contactoRepository.saveAll(contactos)
        return ResponseEntity.ok(mapOf("mensaje" to "Contactos importados exitosamente", "cantidad" to saved.size))
    }

    @GetMapping("/admin/agents")
    fun getAgentes(): ResponseEntity<List<Map<String, String>>> {
        val agentes = usuarioRepository.findAll().filter { it.rol.lowercase() == "agente" }
        val respuesta = agentes.map {
            mapOf(
                "id" to it.id,
                "nombre" to it.nombre,
                "email" to it.email
            )
        }
        return ResponseEntity.ok(respuesta)
    }

    @GetMapping("/admin/surveys")
    fun getEncuestas(): ResponseEntity<List<Encuesta>> =
        ResponseEntity.ok(encuestaRepository.findAll())
}
