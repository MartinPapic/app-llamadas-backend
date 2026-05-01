package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "usuario_lista")
data class UsuarioLista(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val usuarioId: String = "",
    val listaId: String = "",
    val fechaAsignacion: Long = 0L
)
