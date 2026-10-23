package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.response.SecretariatEmployeDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.SecretariatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secretariat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SECRETAIRE')")
public class SecretariatController {
    private final SecretariatService service;

    @GetMapping("/employes")
    public ResponseEntity<ApiResponse<List<SecretariatEmployeDTO>>> employes() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Employés de votre succursale", service.employesDuJour()));
    }

    @PostMapping("/employes/{id}/arrivee")
    public ResponseEntity<ApiResponse<Void>> arrivee(@PathVariable Long id) {
        service.enregistrerArrivee(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Arrivée enregistrée", null));
    }

    @GetMapping(value = "/rapport-mensuel", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> rapportMensuel(@RequestParam String mois) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=presences-" + mois + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.rapportMensuel(mois));
    }
}
