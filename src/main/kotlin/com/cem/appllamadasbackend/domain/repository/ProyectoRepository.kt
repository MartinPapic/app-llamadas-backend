package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Proyecto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProyectoRepository : JpaRepository<Proyecto, String>
