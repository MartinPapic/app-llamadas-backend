package com.cem.appllamadasbackend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import com.cem.appllamadasbackend.domain.model.RolUsuario
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long,
    @Value("\${jwt.refresh-expiration-ms}") private val refreshExpirationMs: Long
) {
    private val signingKey by lazy {
        val keyBytes = Base64.getDecoder().decode(secret)
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(email: String, rol: RolUsuario): String =
        buildToken(email, rol, expirationMs)

    fun generateRefreshToken(email: String, rol: RolUsuario): String =
        buildToken(email, rol, refreshExpirationMs)

    private fun buildToken(subject: String, rol: RolUsuario, expiration: Long): String =
        Jwts.builder()
            .setSubject(subject)
            .claim("rol", rol.name)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()

    fun extractEmail(token: String): String =
        extractClaims(token).subject

    fun extractRol(token: String): String =
        extractClaims(token).get("rol", String::class.java)

    fun isValid(token: String): Boolean = try {
        extractClaims(token).expiration.after(Date())
    } catch (e: Exception) {
        false
    }

    private fun extractClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .body
}
