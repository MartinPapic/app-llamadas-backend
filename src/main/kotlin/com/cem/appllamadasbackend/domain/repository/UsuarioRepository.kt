package com.cem.appllamadasbackend.domain.repository

import com.cem.appllamadasbackend.domain.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UsuarioRepository : JpaRepository<Usuario, String> {
    fun findByEmail(email: String): Optional<Usuario>
}
