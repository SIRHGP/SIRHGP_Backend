package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.AuthRequestDTO;
import com.example.backend_sirhgp.dto.request.EntrepriseSignupDTO;
import com.example.backend_sirhgp.dto.request.EmployerCreateDTO;
import com.example.backend_sirhgp.dto.response.AuthResponseDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.dto.response.LogoutResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.security.JwtService;
import com.example.backend_sirhgp.service.AuthService;
import com.example.backend_sirhgp.service.TokenBlacklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j  // ← AJOUTÉ pour pouvoir utiliser log.error()
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody AuthRequestDTO request) {
        try {
            AuthResponseDTO response = authService.authenticate(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Authentification réussie", response));
        } catch (AuthenticationServiceException e) {
            log.error("Service d'authentification indisponible pour {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Le service de connexion est temporairement indisponible", null));
        } catch (AuthenticationException e) {
            // Ne pas révéler si l'adresse existe, mais conserver une réponse distincte des erreurs serveur.
            log.warn("Échec d'authentification pour {} ({})", request.getEmail(), e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Identifiants incorrects", null));
        }
    }

    @PostMapping("/register-entreprise")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> registerEntreprise(
            @Valid @RequestBody EntrepriseSignupDTO request) {
        EmployerResponseDTO response = authService.registerEntreprise(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Entreprise et compte administrateur créés avec succès", response));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> register(
            @Valid @RequestBody EmployerCreateDTO dto) {
        EmployerResponseDTO created = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Compte créé avec succès", created));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> getProfile() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Profil récupéré avec succès",
                        authService.getCurrentUserProfile()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponseDTO>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        LogoutResponseDTO response = new LogoutResponseDTO();
        response.setTimestamp(LocalDateTime.now());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtService.extractUsername(token);
                tokenBlacklistService.blacklistToken(token);
                response.setEmail(email);
                response.setMessage("Token invalidé avec succès");
            } catch (Exception e) {
                response.setMessage("Token invalide — déconnexion effectuée quand même");
            }
        } else {
            response.setMessage("Déconnexion sans token");
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Déconnexion réussie", response));
    }
}
