package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Usuario
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import com.cem.appllamadasbackend.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.UUID

// ─── DTOs ────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val rol: String,
    val nombre: String,
    val userId: String
)

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: String = "agente"
)

// ─── Controller ──────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/auth")
class AuthController(
    private val usuarioRepository: UsuarioRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * POST /api/auth/login
     * Recibe email + password, valida credenciales y devuelve access + refresh token.
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<*> {
        val usuario = usuarioRepository.findByEmail(request.email).orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Credenciales inválidas"))

        if (!passwordEncoder.matches(request.password, usuario.passwordHash)) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Credenciales inválidas"))
        }

        val accessToken  = jwtService.generateToken(usuario.email, usuario.rol)
        val refreshToken = jwtService.generateRefreshToken(usuario.email, usuario.rol)

        return ResponseEntity.ok(
            LoginResponse(
                accessToken  = accessToken,
                refreshToken = refreshToken,
                rol          = usuario.rol,
                nombre       = usuario.nombre,
                userId       = usuario.id
            )
        )
    }

    /**
     * POST /api/auth/register
     * Crea un usuario nuevo (solo para setup inicial / admin).
     * En producción Esta ruta debería estar protegida o eliminada.
     */
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<*> {
        if (usuarioRepository.findByEmail(request.email).isPresent) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "El email ya está registrado"))
        }

        val usuario = Usuario(
            id           = UUID.randomUUID().toString(),
            nombre       = request.nombre,
            email        = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            rol          = request.rol
        )
        usuarioRepository.save(usuario)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("mensaje" to "Usuario creado correctamente", "id" to usuario.id))
    }
}
