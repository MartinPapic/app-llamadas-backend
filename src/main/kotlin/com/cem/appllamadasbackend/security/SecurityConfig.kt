package com.cem.appllamadasbackend.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtFilter: JwtFilter) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    // Rutas de lectura del dashboard — solo requieren JWT válido (cualquier rol)
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/metrics").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/admin/**").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/analytics/**").authenticated()
                    // Escritura: solo administradores
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/admin/**").hasRole("ADMIN")
                    .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/admin/**").hasRole("ADMIN")
                    .requestMatchers("/usuarios/**").hasRole("ADMIN")
                    // el resto requiere autenticación
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
