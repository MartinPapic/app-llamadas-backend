package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class Tipificacion(
    @Id
    val id: String = "",
    val nombre: String = "",
    val resultado: String = "", // CONTACTADO_EFECTIVO, CONTACTADO_NO_EFECTIVO, NO_CONTACTADO
    val cierraCaso: Boolean = false
)
