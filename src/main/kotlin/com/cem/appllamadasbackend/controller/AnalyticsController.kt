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
import com.cem.appllamadasbackend.domain.repository.ProyectoRepository
import com.cem.appllamadasbackend.domain.repository.ListaRepository
import java.time.Instant
import java.time.format.DateTimeFormatter

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

data class MetricasResponse(
    val totalContactos: Long,
    val totalLlamadas: Long,
    val totalLlamadasValidas: Long,
    val totalContestan: Long,
    val totalNoContestan: Long,
    val duracionPromedio: Double,
    val tasaContacto: Double,
    val distribucionTipificaciones: Map<String, Double>
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ExportDataDto(
    val event_id: String,
    val event_type: String,
    val event_timestamp: Long,
    val record_id: String,
    val record_phone: String,
    val record_name: String,
    val group_id: String?,
    val group_name: String?,
    val sub_group_id: String?,
    val sub_group_name: String?,
    val user_id: String,
    val user_name: String,
    val attempt_number: Int,
    val is_valid_attempt: Boolean,
    val attempt_date: String,
    val duration_seconds: Int?,
    val duration_minutes: Double?,
    val classification: String?,
    val is_closing_classification: Boolean,
    val classification_reverted: Boolean,
    val record_status: String,
    val closure_reason: String?,
    val total_valid_attempts: Int,
    val is_blocked: Boolean,
    val is_callable: Boolean,
    val previous_event_id: String?,
    val action_source: String,
    val comments: String?
)

@RestController
@RequestMapping
class AnalyticsController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val usuarioRepository: UsuarioRepository,
    private val proyectoRepository: ProyectoRepository,
    private val listaRepository: ListaRepository
) {

    @GetMapping("/metrics")
    fun getMetricas(@RequestParam(required = false) proyectoId: String?): ResponseEntity<MetricasResponse> {
        val totalContactos = if (proyectoId != null) contactoRepository.findAll().count { it.proyectoId == proyectoId }.toLong() 
                            else contactoRepository.count()
        
        val todasLlamadas = if (proyectoId != null) llamadaRepository.findAll().filter { it.proyectoId == proyectoId }
                            else llamadaRepository.findAll()
                            
        val llamadasValidas = todasLlamadas.filter { it.intentoValido != false }
                            
        val totalLlamadas = todasLlamadas.size.toLong()
        val totalLlamadasValidas = llamadasValidas.size.toLong()
        val contestan = llamadasValidas.count { it.resultado == ResultadoLlamada.CONTACTADO_EFECTIVO || it.resultado == ResultadoLlamada.CONTACTADO_NO_EFECTIVO }.toLong()
        val noContestan = llamadasValidas.count { it.resultado == ResultadoLlamada.NO_CONTACTADO }.toLong()
        val durPromedio = llamadasValidas.mapNotNull { it.duracion }.let {
            if (it.isEmpty()) 0.0 else it.average()
        }
        val contestanEfectivos = llamadasValidas.count { it.resultado == ResultadoLlamada.CONTACTADO_EFECTIVO }.toDouble()
        val tasa = if (totalLlamadasValidas > 0) (contestanEfectivos / totalLlamadasValidas) * 100 else 0.0

        val distribucionTipificaciones = llamadasValidas.filter { it.tipificacion != null }
            .groupBy { it.tipificacion!! }
            .mapValues { (_, v) -> if (totalLlamadasValidas > 0) (v.size.toDouble() / totalLlamadasValidas) * 100 else 0.0 }

        return ResponseEntity.ok(MetricasResponse(
            totalContactos = totalContactos,
            totalLlamadas = totalLlamadas,
            totalLlamadasValidas = totalLlamadasValidas,
            totalContestan = contestan,
            totalNoContestan = noContestan,
            duracionPromedio = durPromedio,
            tasaContacto = tasa,
            distribucionTipificaciones = distribucionTipificaciones
        ))
    }

    @GetMapping("/analytics/realtime")
    fun getRealtimeMetrics(
        @RequestParam(required = false) fecha: String?,
        @RequestParam(required = false) proyectoId: String?
    ): ResponseEntity<Map<String, Any>> {
        val date = fecha?.let { try { LocalDate.parse(it) } catch (e: Exception) { LocalDate.now(ZoneId.of("America/Santiago")) } } ?: LocalDate.now(ZoneId.of("America/Santiago"))
        val startOfDay = date.atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli()

        var llamadasDeHoy = llamadaRepository.findAll().filter { it.fechaInicio >= startOfDay }
        if (proyectoId != null) {
            llamadasDeHoy = llamadasDeHoy.filter { it.proyectoId == proyectoId }
        }
        val llamadasValidasDeHoy = llamadasDeHoy.filter { it.intentoValido != false }
        val llamadasEmitidasHoyValidas = llamadasValidasDeHoy.size.toLong()
        
        val agentesActivos = llamadasDeHoy.map { it.usuarioId }.distinct().count()
        val distribucion = llamadasValidasDeHoy.groupBy { it.resultado?.name ?: "UNKNOWN" }.mapValues { it.value.size.toLong() }
        
        val contestan = (distribucion[ResultadoLlamada.CONTACTADO_EFECTIVO.name] ?: 0L) + (distribucion[ResultadoLlamada.CONTACTADO_NO_EFECTIVO.name] ?: 0L)
        val tasaContactabilidad = if (llamadasEmitidasHoyValidas > 0) (contestan.toDouble() / llamadasEmitidasHoyValidas) * 100 else 0.0

        val response = mapOf(
            "llamadasEmitidasHoy" to llamadasEmitidasHoyValidas,
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
    fun getAgentStats(@RequestParam(required = false) proyectoId: String?): ResponseEntity<List<Map<String, Any>>> {
        val llamadasTodas = if (proyectoId != null) llamadaRepository.findAll().filter { it.proyectoId == proyectoId } 
                       else llamadaRepository.findAll()
        val llamadas = llamadasTodas.filter { it.intentoValido != false }
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
    fun exportData(
        @RequestParam(required = false) proyectoId: String?,
        @RequestParam(required = false) agenteId: String?,
        @RequestParam(required = false) fechaInicio: String?,
        @RequestParam(required = false) fechaFin: String?
    ): ResponseEntity<List<ExportDataDto>> {
        var llamadas = llamadaRepository.findAll().asSequence()
        if (proyectoId != null) llamadas = llamadas.filter { it.proyectoId == proyectoId }
        if (agenteId != null) llamadas = llamadas.filter { it.usuarioId == agenteId }
        if (fechaInicio != null) {
            val start = LocalDate.parse(fechaInicio).atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli()
            llamadas = llamadas.filter { it.fechaInicio >= start }
        }
        if (fechaFin != null) {
            val end = LocalDate.parse(fechaFin).plusDays(1).atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli()
            llamadas = llamadas.filter { it.fechaInicio < end }
        }
        val llamadasList = llamadas.toList()
                       
        // Optimización: cargar en memoria IDs necesarios
        val contactosIds = llamadasList.map { it.contactoId }.toSet()
        val contactosMap = contactoRepository.findAllById(contactosIds).associateBy { it.id }
        
        val usuariosIds = llamadasList.map { it.usuarioId }.toSet()
        val usuariosMap = usuarioRepository.findAllById(usuariosIds).associateBy { it.id }

        val todasLlamadasContactos = llamadaRepository.findAll().filter { it.contactoId in contactosIds }
        val llamadasPorContacto = todasLlamadasContactos.groupBy { it.contactoId }.mapValues { (_, calls) -> calls.sortedBy { it.fechaInicio } }

        val proyectosIds = llamadasList.mapNotNull { it.proyectoId }.toSet() + contactosMap.values.mapNotNull { it.proyectoId }.toSet()
        val proyectosMap = proyectoRepository.findAllById(proyectosIds).associateBy { it.id }

        val listasIds = llamadasList.mapNotNull { it.listaId }.toSet() + contactosMap.values.mapNotNull { it.listaId }.toSet()
        val listasMap = listaRepository.findAllById(listasIds).associateBy { it.id }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/Santiago"))

        val exportList = llamadasList.mapNotNull { llamada ->
            val contacto = contactosMap[llamada.contactoId] ?: return@mapNotNull null
            val agente = usuariosMap[llamada.usuarioId]
            val proyecto = proyectosMap[llamada.proyectoId ?: contacto.proyectoId]
            val lista = listasMap[llamada.listaId ?: contacto.listaId]
            
            val callsOfContact = llamadasPorContacto[contacto.id] ?: emptyList()
            val index = callsOfContact.indexOfFirst { it.id == llamada.id }
            val attemptNumber = if (index >= 0) index + 1 else 1
            val prevEventId = if (index > 0) callsOfContact[index - 1].id else null
            
            ExportDataDto(
                event_id = llamada.id,
                event_type = "LLAMADA",
                event_timestamp = llamada.fechaInicio,
                record_id = contacto.referenciaId ?: contacto.id,
                record_phone = contacto.telefono,
                record_name = contacto.nombre,
                group_id = proyecto?.id,
                group_name = proyecto?.nombre,
                sub_group_id = lista?.id,
                sub_group_name = lista?.nombre,
                user_id = llamada.usuarioId,
                user_name = agente?.nombre ?: agente?.email ?: "Desconocido",
                attempt_number = attemptNumber,
                is_valid_attempt = llamada.intentoValido ?: true,
                attempt_date = formatter.format(Instant.ofEpochMilli(llamada.fechaInicio)),
                duration_seconds = llamada.duracion,
                duration_minutes = llamada.duracion?.let { it / 60.0 },
                classification = llamada.tipificacion,
                is_closing_classification = contacto.estado.name == "CERRADO" || contacto.estado.name == "CERRADO_POR_INTENTOS" || contacto.estado.name == "DESISTIDO" || contacto.estado.name == "CONTACTADO",
                classification_reverted = false,
                record_status = contacto.estado.name,
                closure_reason = llamada.motivo,
                total_valid_attempts = contacto.intentosValidos ?: 0,
                is_blocked = (contacto.intentosValidos ?: 0) >= 5 || contacto.estado.name == "CERRADO_POR_INTENTOS",
                is_callable = contacto.estado.name == "PENDIENTE" || contacto.estado.name == "EN_GESTION",
                previous_event_id = prevEventId,
                action_source = "APP_ANDROID",
                comments = llamada.observacion
            )
        }.sortedBy { it.event_timestamp }

        return ResponseEntity.ok(exportList)
    }
}
