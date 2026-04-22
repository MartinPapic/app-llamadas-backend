package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "proyecto")
data class Proyecto(
    @Id
    val id: String = "",
    val nombre: String = "",
    val instrumentoUrl: String = "", // URL de QuestionPro
    val fechaCreacion: Long = System.currentTimeMillis()
)
