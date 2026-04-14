package com.cem.appllamadasbackend.domain.model

enum class RolUsuario {
    ADMIN, AGENTE;

    fun toSpringRole() = "ROLE_${this.name}"
    fun toDbValue() = this.name.lowercase()

    companion object {
        fun fromString(value: String): RolUsuario =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Rol inválido: '$value'. Valores permitidos: ADMIN, AGENTE")
    }
}
