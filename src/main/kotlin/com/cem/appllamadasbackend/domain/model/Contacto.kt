package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "contacto")
data class Contacto(
    @Id
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val estado: String = "", // pendiente, en_gestion, contactado, desistido
    val intentos: Int = 0,
    val fechaCreacion: Long = 0L
)
