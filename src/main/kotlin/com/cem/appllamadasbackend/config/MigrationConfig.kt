package com.cem.appllamadasbackend.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.slf4j.LoggerFactory

@Configuration
class MigrationConfig(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(MigrationConfig::class.java)

    @Bean
    fun runMigration() = CommandLineRunner {
        logger.info("Iniciando migración de datos para compatibilidad jerárquica...")
        
        try {
            // 1. Convertir CONTESTA -> CONTACTADO_EFECTIVO + Tipificacion Default
            val contestadas = jdbcTemplate.update(
                "UPDATE llamada SET resultado = 'CONTACTADO_EFECTIVO', tipificacion = 'ENCUESTA_COMPLETA' WHERE resultado = 'CONTESTA'"
            )
            if (contestadas > 0) logger.info("Migrados $contestadas registros de CONTESTA a CONTACTADO_EFECTIVO/ENCUESTA_COMPLETA")

            // 2. Convertir NO_CONTESTA / OCUPADO / INVALIDO -> NO_CONTACTADO + Tipificacion Default
            val noContestadas = jdbcTemplate.update(
                "UPDATE llamada SET resultado = 'NO_CONTACTADO', tipificacion = 'NO_CONTESTA' WHERE resultado IN ('NO_CONTESTA', 'OCUPADO', 'INVALIDO', 'NUMERO_INVALIDO')"
            )
            if (noContestadas > 0) logger.info("Migrados $noContestadas registros de estados fallidos a NO_CONTACTADO/NO_CONTESTA")

            logger.info("Migración completada exitosamente.")
        } catch (e: Exception) {
            logger.error("Error durante la migración de datos: ${e.message}")
            // No bloqueamos el arranque, tal vez la tabla aún no existe o ya está limpia
        }
    }
}
