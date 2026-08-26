package com.tidsec.novaeyetech_backend.controller;

import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    public record HealthStatus(String message, LocalDateTime timestamp) {
    }

    @GetMapping("/")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus("NovaEyeTech backend operativo", LocalDateTime.now()));
    }
}
