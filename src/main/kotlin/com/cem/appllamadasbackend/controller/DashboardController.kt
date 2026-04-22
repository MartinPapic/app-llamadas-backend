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
import com.cem.appllamadasbackend.domain.model.ResultadoLlamada
import com.cem.appllamadasbackend.domain.model.RolUsuario

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
    fun getMetricas(@RequestParam(required = false) proyectoId: String?): ResponseEntity<MetricasResponse> {
        val totalContactos = if (proyectoId != null) contactoRepository.findAll().count { it.proyectoId == proyectoId }.toLong() 
                            else contactoRepository.count()
        
        val todasLlamadas  = if (proyectoId != null) llamadaRepository.findAll().filter { it.proyectoId == proyectoId }
                            else llamadaRepository.findAll()
                            
        val totalLlamadas  = todasLlamadas.size.toLong()
        val contestan      = todasLlamadas.count { it.resultado == ResultadoLlamada.CONTACTADO_EFECTIVO || it.resultado == ResultadoLlamada.CONTACTADO_NO_EFECTIVO }.toLong()
        val noContestan    = todasLlamadas.count { it.resultado == ResultadoLlamada.NO_CONTACTADO }.toLong()
        val durPromedio    = todasLlamadas.mapNotNull { it.duracion }.let {
            if (it.isEmpty()) 0.0 else it.average()
        }
        val contestanEfectivos = todasLlamadas.count { it.resultado == ResultadoLlamada.CONTACTADO_EFECTIVO }.toDouble()
        val tasa = if (totalLlamadas > 0) (contestanEfectivos / totalLlamadas) * 100 else 0.0

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
    fun uploadContactos(
        @RequestBody contactos: List<Contacto>,
        @RequestParam(required = false) proyectoId: String?
    ): ResponseEntity<Map<String, Any>> {
        val contactosAProcesar = if (proyectoId != null) {
            contactos.map { it.copy(proyectoId = proyectoId) }
        } else {
            contactos
        }
        val saved = contactoRepository.saveAll(contactosAProcesar)
        return ResponseEntity.ok(mapOf("mensaje" to "Contactos importados exitosamente", "cantidad" to saved.size))
    }

    @GetMapping("/admin/agents")
    fun getAgentes(): ResponseEntity<List<Map<String, String>>> {
        // Enviar todos (ADMIN y AGENTE) para que el CSV pueda emparejar contactos a admins testeando.
        val agentes = usuarioRepository.findAll()
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
