package com.cem.appllamadasbackend.service

import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScheduledTasks(
    private val contactoRepository: ContactoRepository
) {
    private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)

    // Se ejecuta todos los días a las 02:00 AM hora de Chile
    @Scheduled(cron = "0 0 2 * * *", zone = "America/Santiago")
    @Transactional
    fun releaseStuckContactsDaily() {
        logger.info("Iniciando tarea programada: releaseStuckContactsDaily")
        try {
            val affectedRows = contactoRepository.unlockDailyContacts()
            logger.info("Tarea programada finalizada con éxito. Contactos liberados: $affectedRows")
        } catch (e: Exception) {
            logger.error("Error al ejecutar releaseStuckContactsDaily", e)
        }
    }
}
