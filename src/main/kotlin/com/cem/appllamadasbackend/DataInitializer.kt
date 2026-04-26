package com.cem.appllamadasbackend

import com.cem.appllamadasbackend.domain.model.RolUsuario
import com.cem.appllamadasbackend.domain.model.Usuario
import com.cem.appllamadasbackend.domain.model.Tipificacion
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import com.cem.appllamadasbackend.domain.repository.TipificacionRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.UUID

import org.springframework.jdbc.core.JdbcTemplate

/**
 * Siembra un usuario administrador por defecto al arrancar si no existe ninguno.
 * Credenciales: admin@cem.cl / REMOVED_SECRET
 * Cambia la contraseña desde el panel de administración tras el primer acceso.
 */
@Component
class DataInitializer(
    private val usuarioRepository: UsuarioRepository,
    private val tipificacionRepository: TipificacionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jdbcTemplate: JdbcTemplate
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            // Limpia los datos existentes (de pruebas antiguas) conviertiendo todo a mayusculas
            // para que los mappings strictos de enum en JPA no crasheen con 500
            jdbcTemplate.execute("UPDATE usuario SET rol = UPPER(rol)")
            jdbcTemplate.execute("UPDATE contacto SET estado = UPPER(estado)")
            // llamada puede tener resultado null, así que ignorar nulls para no romper cosas
            jdbcTemplate.execute("UPDATE llamada SET resultado = UPPER(resultado) WHERE resultado IS NOT NULL")
            jdbcTemplate.execute("UPDATE encuesta SET estado = UPPER(estado) WHERE estado IS NOT NULL")
            println("✅ [DataInitializer] Integridad de datos completada (Enums actualizados a UPPERCASE)")
        } catch (e: Exception) {
            System.err.println("⚠️ [DataInitializer] Falló limpiar enums de la base de datos: ${e.message}")
        }
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

        // --- Seeding de Tipificaciones ---
        if (tipificacionRepository.count() == 0L) {
            val tipificaciones = listOf(
                Tipificacion(UUID.randomUUID().toString(), "Contactado", "CONTACTADO_EFECTIVO", false),
                Tipificacion(UUID.randomUUID().toString(), "Enviar Mail", "CONTACTADO_EFECTIVO", false),
                Tipificacion(UUID.randomUUID().toString(), "Llamar más tarde", "CONTACTADO_NO_EFECTIVO", false),
                Tipificacion(UUID.randomUUID().toString(), "No quiere participar", "CONTACTADO_NO_EFECTIVO", true),
                Tipificacion(UUID.randomUUID().toString(), "Desistió del proyecto/programa", "CONTACTADO_NO_EFECTIVO", true),
                Tipificacion(UUID.randomUUID().toString(), "Desconfianza de la encuesta telefónica", "CONTACTADO_NO_EFECTIVO", true),
                Tipificacion(UUID.randomUUID().toString(), "No contesta", "NO_CONTACTADO", false),
                Tipificacion(UUID.randomUUID().toString(), "Fuera de Servicio", "NO_CONTACTADO", false),
                Tipificacion(UUID.randomUUID().toString(), "Teléfono Apagado", "NO_CONTACTADO", false),
                Tipificacion(UUID.randomUUID().toString(), "Número no corresponde", "NO_CONTACTADO", true),
                Tipificacion(UUID.randomUUID().toString(), "Número no existe", "NO_CONTACTADO", true)
            )
            tipificacionRepository.saveAll(tipificaciones)
            println("✅ [DataInitializer] Tipificaciones inicializadas")
        }
    }
}
