package com.cem.appllamadasbackend.domain.model

import com.fasterxml.jackson.annotation.JsonCreator

enum class EstadoContacto {
    PENDIENTE,
    EN_GESTION,
    CONTACTADO,
    DESISTIDO,
    CERRADO,
    CERRADO_POR_INTENTOS;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): EstadoContacto? {
            if (value == null) return null
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.replace("_", "").equals(value.replace("_", ""), ignoreCase = true) }
        }
    }
}
