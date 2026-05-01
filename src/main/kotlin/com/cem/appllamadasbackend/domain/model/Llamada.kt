package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType

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
    @Enumerated(EnumType.STRING)
    val resultado: ResultadoLlamada? = null,
    val tipificacion: String? = null,
    val motivo: String? = null,
    val observacion: String? = null,
    val proyectoId: String? = null,
    val listaId: String? = null,
    val intentoValido: Boolean = true
)
