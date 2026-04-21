package com.example.backend_sirhgp.security;

import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployerRepository employerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email.toLowerCase().trim();
        log.debug("Recherche de l'utilisateur : {}", normalized);

        Employer employer = employerRepository.findByEmailWithFullContext(normalized)
                .orElseThrow(() -> {
                    log.error("Utilisateur non trouvé : {}", normalized);
                    return new UsernameNotFoundException("Utilisateur non trouvé : " + email);
                });

        // CORRECTION : évite le crash NullPointerException si role est null
        String roleName = employer.getRole() != null ? employer.getRole().getName() : "AUCUN";
        log.info("Utilisateur trouvé : {} avec rôle {}", employer.getEmail(), roleName);

        return employer;
    }
}