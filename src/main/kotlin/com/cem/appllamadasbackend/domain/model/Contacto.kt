package com.cem.appllamadasbackend.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType

@Entity
@Table(name = "contacto")
data class Contacto(
    @Id
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    @Enumerated(EnumType.STRING)
    var estado: EstadoContacto = EstadoContacto.PENDIENTE,
    var intentos: Int = 0,
    val fechaCreacion: Long = 0L,
    var ultimaTipificacion: String? = null,
    var ultimaObservacion: String? = null,
    val proyectoId: String? = null,
    val listaId: String? = null,
    val referenciaId: String? = null,
    var intentosValidos: Int = 0,
    var agenteId: String? = null,
    var bloqueadoPor: String? = null,
    var fechaBloqueo: Long? = null
)
