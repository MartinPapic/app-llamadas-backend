package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Tipificacion
import com.cem.appllamadasbackend.domain.repository.TipificacionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tipifications")
class TipificacionController(
    private val tipificacionRepository: TipificacionRepository
) {

    @GetMapping
    fun getAll(): ResponseEntity<List<Tipificacion>> {
        return ResponseEntity.ok(tipificacionRepository.findAll())
    }
}
