package com.cem.appllamadasbackend.domain.model

import com.fasterxml.jackson.annotation.JsonCreator

enum class ResultadoLlamada {
    CONTACTADO_EFECTIVO,
    CONTACTADO_NO_EFECTIVO,
    NO_CONTACTADO;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): ResultadoLlamada? {
            if (value == null) return null
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}
