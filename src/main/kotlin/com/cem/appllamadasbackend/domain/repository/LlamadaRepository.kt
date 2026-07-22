package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Llamada
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

import org.springframework.data.jpa.repository.Query

interface ResultadoDistribucion {
    val resultado: com.cem.appllamadasbackend.domain.model.ResultadoLlamada?
    val cantidad: Long
}

interface MetricasDTO {
    val totalLlamadas: Long
    val llamadasValidas: Long
    val contestan: Long
    val noContestan: Long
    val duracionSuma: Long
    val duracionConteo: Long
    val contestanEfectivos: Long
    val totalGestionExitosa: Long
}

interface TipificacionDistribucionDTO {
    val tipificacion: String?
    val cantidad: Long
}

interface AgentStatsDTO {
    val agenteId: String
    val totalLlamadas: Long
    val efectivos: Long
    val noEfectivos: Long
    val noContestan: Long
    val gestionExitosa: Long
    val llamadasCortas: Long
    val duracionAvg: Double?
}

interface RealtimeMetricsDTO {
    val llamadasEmitidasHoyValidas: Long?
    val contactadoEfectivo: Long?
    val contactadoNoEfectivo: Long?
    val gestionExitosaHoy: Long?
}

interface ConteoIntentosDTO {
    val contactoId: String
    val conteo: Long
}

interface ResultadoDistribucionString {
    val resultado: String?
    val cantidad: Long
}

@Repository
interface LlamadaRepository : JpaRepository<Llamada, String> {

    @Query("SELECT l.resultado AS resultado, COUNT(l) AS cantidad FROM Llamada l " +
           "WHERE l.fechaInicio >= :inicioDia GROUP BY l.resultado")
    fun countResultadosDesde(inicioDia: Long): List<ResultadoDistribucion>

    @Query("SELECT COUNT(l) FROM Llamada l WHERE l.fechaInicio >= :inicioDia")
    fun countLlamadasDesde(inicioDia: Long): Long

    @Query("SELECT COUNT(DISTINCT l.usuarioId) FROM Llamada l WHERE l.fechaInicio >= :inicioDia")
    fun countAgentesActivosDesde(inicioDia: Long): Long

    @Query(value = "SELECT l.contacto_id as contactoId, COUNT(l.id) as conteo FROM llamada l WHERE l.contacto_id IN :contactoIds AND l.intento_valido = true GROUP BY l.contacto_id", nativeQuery = true)
    fun countIntentosValidosByContactoIds(@org.springframework.data.repository.query.Param("contactoIds") contactoIds: List<String>): List<ConteoIntentosDTO>

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    fun deleteByListaId(listaId: String)

    @Query(value = """
        SELECT 
            COUNT(l.id) as totalLlamadas,
            SUM(CASE WHEN l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA' THEN 1 ELSE 0 END) as llamadasValidas,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND (l.resultado = 'CONTACTADO_EFECTIVO' OR l.resultado = 'CONTACTADO_NO_EFECTIVO') THEN 1 ELSE 0 END) as contestan,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.resultado = 'NO_CONTACTADO' THEN 1 ELSE 0 END) as noContestan,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.duracion IS NOT NULL THEN l.duracion ELSE 0 END) as duracionSuma,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.duracion IS NOT NULL THEN 1 ELSE 0 END) as duracionConteo,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.resultado = 'CONTACTADO_EFECTIVO' THEN 1 ELSE 0 END) as contestanEfectivos,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.motivo = 'GESTION_EXITOSA' THEN 1 ELSE 0 END) as totalGestionExitosa
        FROM llamada l
        WHERE (:proyectoId IS NULL OR l.proyecto_id = :proyectoId)
    """, nativeQuery = true)
    fun getMetricasGenerales(@org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?): MetricasDTO

    @Query(value = """
        SELECT 
            l.tipificacion as tipificacion,
            COUNT(l.id) as cantidad
        FROM llamada l
        WHERE (:proyectoId IS NULL OR l.proyecto_id = :proyectoId)
          AND (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA')
          AND l.tipificacion IS NOT NULL
        GROUP BY l.tipificacion
    """, nativeQuery = true)
    fun getDistribucionTipificaciones(@org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?): List<TipificacionDistribucionDTO>

    @Query(value = """
        SELECT 
            SUM(CASE WHEN l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA' THEN 1 ELSE 0 END) as llamadasEmitidasHoyValidas,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.resultado = 'CONTACTADO_EFECTIVO' THEN 1 ELSE 0 END) as contactadoEfectivo,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.resultado = 'CONTACTADO_NO_EFECTIVO' THEN 1 ELSE 0 END) as contactadoNoEfectivo,
            SUM(CASE WHEN (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA') AND l.motivo = 'GESTION_EXITOSA' THEN 1 ELSE 0 END) as gestionExitosaHoy
        FROM llamada l
        WHERE (:proyectoId IS NULL OR l.proyecto_id = :proyectoId)
          AND l.fecha_inicio >= :startOfDay
    """, nativeQuery = true)
    fun getRealtimeMetricsStats(
        @org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?,
        @org.springframework.data.repository.query.Param("startOfDay") startOfDay: Long
    ): RealtimeMetricsDTO

    @Query(value = """
        SELECT 
            COALESCE(l.resultado, 'UNKNOWN') as resultado,
            COUNT(l.id) as cantidad
        FROM llamada l
        WHERE (:proyectoId IS NULL OR l.proyecto_id = :proyectoId)
          AND l.fecha_inicio >= :startOfDay
          AND (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA')
        GROUP BY COALESCE(l.resultado, 'UNKNOWN')
    """, nativeQuery = true)
    fun getRealtimeDistribucion(
        @org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?,
        @org.springframework.data.repository.query.Param("startOfDay") startOfDay: Long
    ): List<ResultadoDistribucionString>

    @Query(value = "SELECT COUNT(DISTINCT l.usuario_id) FROM llamada l WHERE (:proyectoId IS NULL OR l.proyecto_id = :proyectoId) AND l.fecha_inicio >= :startOfDay", nativeQuery = true)
    fun countAgentesActivosRealtime(
        @org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?,
        @org.springframework.data.repository.query.Param("startOfDay") startOfDay: Long
    ): Long

    @Query(value = """
        SELECT 
            l.usuario_id as agenteId,
            COUNT(l.id) as totalLlamadas,
            SUM(CASE WHEN l.resultado = 'CONTACTADO_EFECTIVO' THEN 1 ELSE 0 END) as efectivos,
            SUM(CASE WHEN l.resultado = 'CONTACTADO_NO_EFECTIVO' THEN 1 ELSE 0 END) as noEfectivos,
            SUM(CASE WHEN l.resultado = 'NO_CONTACTADO' THEN 1 ELSE 0 END) as noContestan,
            SUM(CASE WHEN l.motivo = 'GESTION_EXITOSA' THEN 1 ELSE 0 END) as gestionExitosa,
            SUM(CASE WHEN l.resultado = 'NO_CONTACTADO' 
                          AND l.duracion < 15 
                          AND (l.tipificacion IS NULL OR l.tipificacion NOT IN ('Teléfono Apagado', 'Número no existe', 'Fuera de Servicio', 'Número no corresponde')) 
                     THEN 1 ELSE 0 END) as llamadasCortas,
            AVG(l.duracion) as duracionAvg
        FROM llamada l
        WHERE (:proyectoId IS NULL OR l.proyecto_id = :proyectoId)
          AND (l.intento_valido = true OR l.motivo = 'GESTION_EXITOSA')
        GROUP BY l.usuario_id
    """, nativeQuery = true)
    fun getAgentStats(@org.springframework.data.repository.query.Param("proyectoId") proyectoId: String?): List<AgentStatsDTO>
}
