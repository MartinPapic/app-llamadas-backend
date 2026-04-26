package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Tipificacion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TipificacionRepository : JpaRepository<Tipificacion, String>
