package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "usuario")
data class Usuario(
    @Id
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val rol: String = "agente"  // agente | admin
)
