package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.AuthRequestDTO;
import com.example.backend_sirhgp.dto.request.EntrepriseSignupDTO;
import com.example.backend_sirhgp.dto.request.EmployerCreateDTO;
import com.example.backend_sirhgp.dto.response.AuthResponseDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.EmployerMapper;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Role;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.EntrepriseRepository;
import com.example.backend_sirhgp.repository.PosteRepository;
import com.example.backend_sirhgp.repository.RoleRepository;
import com.example.backend_sirhgp.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployerRepository employerRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PosteRepository posteRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmployerMapper employerMapper;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Transactional
    public EmployerResponseDTO register(EmployerCreateDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();

        if (employerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        if (dto.getRoleId() != null && isSuperAdminRole(dto.getRoleId())) {
            log.warn("Tentative de création de SUPER_ADMIN via API – bloquée");
            throw new IllegalArgumentException("Création de SUPER_ADMIN interdite via cette API");
        }

        Employer employer = employerMapper.toEntity(dto);
        employer.setEmail(email);
        employer.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        employer.setActif(true);
        if (employer.getDateEmbauche() == null) {
            employer.setDateEmbauche(LocalDate.now());
        }

        if (dto.getPosteId() != null) {
            Poste poste = posteRepository.findById(dto.getPosteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + dto.getPosteId()));
            employer.setPoste(poste);
        }

        if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + dto.getRoleId()));
            employer.setRole(role);
        } else if (employer.getPoste() != null && employer.getPoste().getRole() != null) {
            employer.setRole(employer.getPoste().getRole());
        }

        employer = employerRepository.save(employer);
        log.info("Nouvel employé enregistré : {} ({})", employer.getName(), employer.getEmail());
        return employerMapper.toResponseDTO(employer);
    }

    @Transactional
    public EmployerResponseDTO registerEntreprise(EntrepriseSignupDTO dto) {
        String entrepriseName = dto.getEntrepriseNom().trim();
        String adminEmail = dto.getAdminEmail().trim().toLowerCase();

        if (entrepriseRepository.existsByName(entrepriseName)) {
            throw new IllegalArgumentException("Cette entreprise existe déjà : " + entrepriseName);
        }
        if (employerRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Rôle ADMIN introuvable"));
        Entreprise entreprise = entrepriseRepository.save(Entreprise.builder()
                .name(entrepriseName)
                .secteurActivite(dto.getSecteurActivite())
                .type(dto.getType() != null ? dto.getType() : Entreprise.TypeEntreprise.AUTRE)
                .ville(dto.getVille())
                .telephone(dto.getTelephone())
                .statut(Entreprise.StatutEntreprise.ACTIVE)
                .build());

        Employer admin = Employer.builder()
                .name(dto.getAdminNom().trim())
                .email(adminEmail)
                .motDePasse(passwordEncoder.encode(dto.getMotDePasse()))
                .dateEmbauche(LocalDate.now())
                .actif(true)
                .role(adminRole)
                .entreprise(entreprise)
                .build();

        return employerMapper.toResponseDTO(employerRepository.save(admin));
    }

    public AuthResponseDTO authenticate(AuthRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Tentative de connexion pour : {}", email);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            log.warn("Échec connexion pour {} : mauvais mot de passe", email);
            throw e;
        }

        Employer employer = employerRepository.findByEmailWithFullContext(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        if (!employer.isActif()) {
            throw new DisabledException("Compte désactivé");
        }
        if (employer.getRole() == null || employer.getRole().getName() == null
                || employer.getRole().getName().isBlank()) {
            throw new DisabledException("Aucun rôle n'est attribué à ce compte");
        }

        String token = jwtService.generateToken(employer);

        AuthResponseDTO.UserSummaryDTO userSummary = new AuthResponseDTO.UserSummaryDTO(
                employer.getId(),
                employer.getName(),
                employer.getEmail(),
                employer.getRole() != null ? employer.getRole().getName() : null,
                employer.getPoste() != null ? employer.getPoste().getName() : null,
                employer.getPoste() != null && employer.getPoste().getDepartement() != null
                        ? employer.getPoste().getDepartement().getName() : null,
                employer.isActif()
        );

        return new AuthResponseDTO(token, "Bearer", jwtExpiration, userSummary);
    }

    public EmployerResponseDTO getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Employer employer = employerRepository.findByEmailWithRolesAndPoste(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profil non trouvé pour : " + email));
        return employerMapper.toResponseDTO(employer);
    }

    private boolean isSuperAdminRole(Long roleId) {
        return roleRepository.findById(roleId)
                .map(role -> "SUPER_ADMIN".equalsIgnoreCase(role.getName()))
                .orElse(false);
    }
}
