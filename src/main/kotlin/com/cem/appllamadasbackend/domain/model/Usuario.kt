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
    @Enumerated(EnumType.STRING)
    val rol: RolUsuario = RolUsuario.AGENTE
)
