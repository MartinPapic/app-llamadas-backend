package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.RolUsuario
import com.cem.appllamadasbackend.domain.model.Usuario
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import com.cem.appllamadasbackend.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    val rol: String = "agente"  // Solo "admin" o "agente" son válidos
)

data class RefreshRequest(
    val refreshToken: String
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
                rol          = usuario.rol.name.lowercase(),
                nombre       = usuario.nombre,
                userId       = usuario.id
            )
        )
    }

    /**
     * POST /auth/register — PROTEGIDO: Solo ADMIN puede crear nuevos usuarios.
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<*> {
        val rol = try {
            RolUsuario.fromString(request.rol)
        } catch (e: Exception) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        }

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
            rol          = rol
        )
        usuarioRepository.save(usuario)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("mensaje" to "Usuario creado correctamente", "id" to usuario.id))
    }

    /**
     * POST /auth/refresh — Renueva el access token usando el refresh token.
     */
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<*> {
        if (!jwtService.isValid(request.refreshToken)) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Refresh token inválido o expirado"))
        }

        val email = jwtService.extractEmail(request.refreshToken)
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Usuario no encontrado"))

        val newAccessToken = jwtService.generateToken(usuario.email, usuario.rol)
        return ResponseEntity.ok(mapOf("accessToken" to newAccessToken, "rol" to usuario.rol.name.lowercase()))
    }
}
