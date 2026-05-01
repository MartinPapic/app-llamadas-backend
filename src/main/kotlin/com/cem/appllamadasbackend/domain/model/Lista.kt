package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "lista")
data class Lista(
    @Id
    val id: String = "",
    val nombre: String = "",
    val proyectoId: String = "",
    val fechaCreacion: Long = 0L,
    val estado: String = "ACTIVO"
)
