package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "encuesta")
data class Encuesta(
    @Id
    val id: String = "",
    val contactoId: String = "",
    val url: String = "",
    val estado: String = "", // completa, incompleta, no_realizada
    val fecha: Long = 0L
)
