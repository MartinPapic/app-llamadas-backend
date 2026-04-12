package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Llamada
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LlamadaRepository : JpaRepository<Llamada, String>
