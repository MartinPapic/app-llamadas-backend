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
        val adminPassword = System.getenv("ADMIN_SEED_PASSWORD") ?: return // No crear ni actualizar si no hay variable

        val existente = usuarioRepository.findByEmail(adminEmail)
        if (existente.isPresent) {
            // Actualiza contraseña y rol del admin existente para corregir datos obsoletos
            val admin = existente.get().copy(
                passwordHash = passwordEncoder.encode(adminPassword),
                rol = RolUsuario.ADMIN
            )
            usuarioRepository.save(admin)
            println("✅ [DataInitializer] Admin actualizado: $adminEmail")
        } else {
            val admin = Usuario(
                id = java.util.UUID.randomUUID().toString(),
                nombre = "Administrador CEM",
                email = adminEmail,
                passwordHash = passwordEncoder.encode(adminPassword),
                rol = RolUsuario.ADMIN
            )
            usuarioRepository.save(admin)
            println("✅ [DataInitializer] Usuario admin creado: $adminEmail")
        }
    }
}
