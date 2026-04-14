package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Usuario
import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class UsuarioDTO(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: String
)

data class CrearUsuarioRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: String
)

data class ActualizarUsuarioRequest(
    val nombre: String,
    val email: String,
    val password: String?, // Opcional, si viene vacío no se actualiza
    val rol: String
)

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping
    fun obtenerUsuarios(): ResponseEntity<List<UsuarioDTO>> {
        val usuarios = usuarioRepository.findAll().map { 
            UsuarioDTO(it.id, it.nombre, it.email, it.rol) 
        }
        return ResponseEntity.ok(usuarios)
    }

    @PostMapping
    fun crearUsuario(@RequestBody request: CrearUsuarioRequest): ResponseEntity<*> {
        if (usuarioRepository.findByEmail(request.email).isPresent) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "El email ya está registrado"))
        }

        val usuario = Usuario(
            id = UUID.randomUUID().toString(),
            nombre = request.nombre,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            rol = request.rol
        )
        usuarioRepository.save(usuario)
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UsuarioDTO(usuario.id, usuario.nombre, usuario.email, usuario.rol))
    }

    @PutMapping("/{id}")
    fun actualizarUsuario(
        @PathVariable id: String, 
        @RequestBody request: ActualizarUsuarioRequest
    ): ResponseEntity<*> {
        val usuario = usuarioRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Usuario no encontrado"))

        // Verificar email duplicado (si cambió)
        if (request.email != usuario.email && usuarioRepository.findByEmail(request.email).isPresent) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "El email ya está registrado por otro usuario"))
        }

        val passwordHashToSave = if (!request.password.isNullOrBlank()) {
            passwordEncoder.encode(request.password)
        } else {
            usuario.passwordHash
        }

        val usuarioActualizado = usuario.copy(
            nombre = request.nombre,
            email = request.email,
            rol = request.rol,
            passwordHash = passwordHashToSave
        )
        
        usuarioRepository.save(usuarioActualizado)

        return ResponseEntity.ok(
            UsuarioDTO(
                usuarioActualizado.id, 
                usuarioActualizado.nombre, 
                usuarioActualizado.email, 
                usuarioActualizado.rol
            )
        )
    }

    @DeleteMapping("/{id}")
    fun eliminarUsuario(@PathVariable id: String): ResponseEntity<*> {
        val usuario = usuarioRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Usuario no encontrado"))
        
        try {
            usuarioRepository.delete(usuario)
            return ResponseEntity.ok(mapOf("mensaje" to "Usuario eliminado correctamente"))
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "No se puede eliminar el usuario porque tiene registros dependientes (llamadas, encuestas)."))
        }
    }
}
