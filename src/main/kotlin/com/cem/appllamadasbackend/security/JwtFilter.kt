package com.cem.appllamadasbackend.security

import com.cem.appllamadasbackend.domain.repository.UsuarioRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtFilter(
    private val jwtService: JwtService,
    private val usuarioRepository: UsuarioRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response)
            return
        }

        val email = jwtService.extractEmail(token)

        // Revocación inmediata: verificar que el usuario exista y esté activo
        val usuario = usuarioRepository.findByEmailAndActivoTrue(email).orElse(null)
        if (usuario == null) {
            filterChain.doFilter(request, response)
            return  // No setea SecurityContext → resultará en 401
        }

        val rol   = jwtService.extractRol(token)
        val authName = "ROLE_${rol.uppercase()}"

        // DEBUG: Imprimir el rol extraído y las autoridades inyectadas
        println("🔐 [JwtFilter] Email extraído: $email | Rol extraído: $rol | Autoridad concedida: $authName | Endpoint: ${request.requestURI}")

        val auth = UsernamePasswordAuthenticationToken(
            email,
            null,
            listOf(SimpleGrantedAuthority(authName))
        )
        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = auth

        filterChain.doFilter(request, response)
    }
}
