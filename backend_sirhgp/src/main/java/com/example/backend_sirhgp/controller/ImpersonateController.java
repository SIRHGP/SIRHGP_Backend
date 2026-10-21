package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.ImpersonateRequestDTO;
import com.example.backend_sirhgp.dto.response.ImpersonateMeDTO;
import com.example.backend_sirhgp.dto.response.ImpersonateResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.ImpersonateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Refonte 5.9 (§6) — impersonation (support SUPER_ADMIN).
 * - POST /api/impersonate : SUPER_ADMIN uniquement (matcher SecurityConfig + méthode) ;
 * - GET /api/impersonate/me et POST /api/impersonate/logout : exécutés SOUS le jeton
 *   impersonné (claims injectés en attributs de requête par JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/impersonate")
@RequiredArgsConstructor
public class ImpersonateController {

    private final ImpersonateService impersonateService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ImpersonateResponseDTO>> impersonate(
            @Valid @RequestBody ImpersonateRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Impersonation active (lecture seule, 15 min)",
                impersonateService.impersonate(dto.getEmployerId())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ImpersonateMeDTO>> me(
            @RequestAttribute(value = "impersonatorId", required = false) Long impersonatorId,
            @RequestAttribute(value = "cibleId", required = false) Long cibleId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "État de l'impersonation", impersonateService.me(impersonatorId, cibleId)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestAttribute(value = "impersonatorId", required = false) Long impersonatorId,
            @RequestAttribute(value = "cibleId", required = false) Long cibleId) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        impersonateService.logout(token, impersonatorId, cibleId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Impersonation terminée", null));
    }
}