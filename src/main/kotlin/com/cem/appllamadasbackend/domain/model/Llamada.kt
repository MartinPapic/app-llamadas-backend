package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "llamada")
data class Llamada(
    @Id
    val id: String = "",
    val contactoId: String = "",
    val usuarioId: String = "",
    val fechaInicio: Long = 0L,
    val fechaFin: Long? = null,
    val duracion: Int? = null,
    val resultado: String? = null,
    val tipificacion: String? = null,
    val observacion: String? = null
)
