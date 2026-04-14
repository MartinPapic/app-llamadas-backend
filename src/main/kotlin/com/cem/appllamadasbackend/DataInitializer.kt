package com.cem.appllamadasbackend

import com.cem.appllamadasbackend.domain.model.RolUsuario
import com.cem.appllamadasbackend.domain.model.Usuario
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Siembra un usuario administrador por defecto al arrancar si no existe ninguno.
 * Credenciales: admin@cem.cl / REMOVED_SECRET
 * Cambia la contraseña desde el panel de administración tras el primer acceso.
 */
@Component
class DataInitializer(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val adminEmail = System.getenv("ADMIN_SEED_EMAIL") ?: "admin@cem.cl"
        val adminPassword = System.getenv("ADMIN_SEED_PASSWORD") ?: "REMOVED_SECRET"

        if (usuarioRepository.findByEmail(adminEmail).isEmpty) {
            val admin = Usuario(
                id = UUID.randomUUID().toString(),
                nombre = "Administrador CEM",
                email = adminEmail,
                passwordHash = passwordEncoder.encode(adminPassword),
                rol = RolUsuario.ADMIN  // JwtFilter lo convierte a ROLE_ADMIN
            )
            usuarioRepository.save(admin)
            println("✅ [DataInitializer] Usuario admin creado: $adminEmail")
        } else {
            println("ℹ️ [DataInitializer] Usuario admin ya existe, omitiendo seed.")
        }
    }
}
