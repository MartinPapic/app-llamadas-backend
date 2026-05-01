package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import com.cem.appllamadasbackend.domain.model.ResultadoLlamada
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository

data class MetricasResponse(
    val totalContactos: Long,
    val totalLlamadas: Long,
    val totalContestan: Long,
    val totalNoContestan: Long,
    val duracionPromedio: Double,
    val tasaContacto: Double
)

data class ExportDataDto(
    val llamadaId: String,
    val contactoId: String,
    val listaId: String?,
    val referenciaId: String?,
    val nombreContacto: String,
    val telefonoContacto: String,
    val fechaLlamada: Long,
    val duracion: Int?,
    val agenteId: String,
    val emailAgente: String,
    val resultado: String?,
    val tipificacion: String?,
    val motivo: String?,
    val observacion: String?,
    val intentoValido: Boolean
)

@RestController
@RequestMapping
class AnalyticsController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val usuarioRepository: UsuarioRepository
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

    @GetMapping("/analytics/realtime")
    fun getRealtimeMetrics(@RequestParam(required = false) fecha: String?): ResponseEntity<Map<String, Any>> {
        val date = fecha?.let { try { LocalDate.parse(it) } catch (e: Exception) { LocalDate.now(ZoneId.of("America/Santiago")) } } ?: LocalDate.now(ZoneId.of("America/Santiago"))
        val startOfDay = date.atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli()

        val llamadasEmitidasHoy = llamadaRepository.countLlamadasDesde(startOfDay)
        val agentesActivos = llamadaRepository.countAgentesActivosDesde(startOfDay)
        val resultados = llamadaRepository.countResultadosDesde(startOfDay)
        val distribucion = resultados.associate { (it.resultado?.name ?: "UNKNOWN") to it.cantidad }
        
        val contestan = (distribucion[ResultadoLlamada.CONTACTADO_EFECTIVO.name] ?: 0L) + (distribucion[ResultadoLlamada.CONTACTADO_NO_EFECTIVO.name] ?: 0L)
        val tasaContactabilidad = if (llamadasEmitidasHoy > 0) (contestan.toDouble() / llamadasEmitidasHoy) * 100 else 0.0

        val response = mapOf(
            "llamadasEmitidasHoy" to llamadasEmitidasHoy,
            "tasaContactabilidadDiaria" to tasaContactabilidad,
            "distribucionResultados" to distribucion,
            "totalAgentesActivos" to agentesActivos
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/analytics/funnel")
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

    @GetMapping("/analytics/agents")
    fun getAgentStats(): ResponseEntity<List<Map<String, Any>>> {
        val llamadas = llamadaRepository.findAll()
        val stats = llamadas.groupBy { it.usuarioId }.map { (usuarioId, calls) ->
            val total = calls.size
            val efectivos = calls.count { it.resultado == ResultadoLlamada.CONTACTADO_EFECTIVO }
            val noEfectivos = calls.count { it.resultado == ResultadoLlamada.CONTACTADO_NO_EFECTIVO }
            val noContestan = calls.count { it.resultado == ResultadoLlamada.NO_CONTACTADO }
            val duracionAvg = if (total > 0) calls.mapNotNull { it.duracion }.average() else 0.0
            
            mapOf(
                "agenteId" to usuarioId,
                "totalLlamadas" to total,
                "efectivos" to efectivos,
                "noEfectivos" to noEfectivos,
                "noContestan" to noContestan,
                "duracionPromedio" to duracionAvg,
                "tasaEfectividad" to if (total > 0) (efectivos.toDouble() / total) * 100 else 0.0
            )
        }
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/analytics/export")
    fun exportData(@RequestParam(required = false) proyectoId: String?): ResponseEntity<List<ExportDataDto>> {
        val llamadas = if (proyectoId != null) llamadaRepository.findAll().filter { it.proyectoId == proyectoId }
                       else llamadaRepository.findAll()
                       
        // Optimización: cargar en memoria IDs necesarios
        val contactosIds = llamadas.map { it.contactoId }.toSet()
        val contactosMap = contactoRepository.findAllById(contactosIds).associateBy { it.id }
        
        val usuariosIds = llamadas.map { it.usuarioId }.toSet()
        val usuariosMap = usuarioRepository.findAllById(usuariosIds).associateBy { it.id }

        val exportList = llamadas.mapNotNull { llamada ->
            val contacto = contactosMap[llamada.contactoId] ?: return@mapNotNull null
            val agente = usuariosMap[llamada.usuarioId]
            
            ExportDataDto(
                llamadaId = llamada.id,
                contactoId = contacto.id,
                listaId = llamada.listaId ?: contacto.listaId,
                referenciaId = contacto.referenciaId,
                nombreContacto = contacto.nombre,
                telefonoContacto = contacto.telefono,
                fechaLlamada = llamada.fechaInicio,
                duracion = llamada.duracion,
                agenteId = llamada.usuarioId,
                emailAgente = agente?.email ?: "Desconocido",
                resultado = llamada.resultado?.name,
                tipificacion = llamada.tipificacion,
                motivo = llamada.motivo,
                observacion = llamada.observacion,
                intentoValido = llamada.intentoValido ?: true
            )
        }.sortedByDescending { it.fechaLlamada }

        return ResponseEntity.ok(exportList)
    }
}
