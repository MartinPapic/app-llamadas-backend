package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Encuesta
import org.springframework.data.jpa.repository.JpaRepository

interface EncuestaRepository : JpaRepository<Encuesta, String>
