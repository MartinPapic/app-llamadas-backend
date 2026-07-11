package com.cem.appllamadasbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

// Trigger redeploy: Hierarchical Typification & Pool Model Sync
@SpringBootApplication
@EnableScheduling
class AppLlamadasBackendApplication

fun main(args: Array<String>) {
    runApplication<AppLlamadasBackendApplication>(*args)
}
