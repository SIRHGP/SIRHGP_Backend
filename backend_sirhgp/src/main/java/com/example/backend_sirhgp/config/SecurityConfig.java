package com.example.backend_sirhgp.config;

import com.example.backend_sirhgp.security.CustomUserDetailsService;
import com.example.backend_sirhgp.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:4200}")
    private String allowedOrigins;

    private DaoAuthenticationProvider buildAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(buildAuthenticationProvider());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "Accept", "Origin", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register-entreprise",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/error"
                        ).permitAll()

                        // Routes spécifiques par rôle
                        .requestMatchers("/api/auth/register").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/auth/profile", "/api/auth/logout").authenticated()

                        // SUPER_ADMIN uniquement
                        .requestMatchers(HttpMethod.GET, "/api/roles/filtres")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "LEAD")
                        .requestMatchers(
                                "/api/roles/**",
                                "/api/permissions/**",
                                "/api/actions/**"
                        ).hasRole("SUPER_ADMIN")

                        // SUPER_ADMIN et ADMIN
                        .requestMatchers("/api/employers/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "LEAD")
                        .requestMatchers(HttpMethod.GET, "/api/departements/**", "/api/postes/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "LEAD")
                        .requestMatchers("/api/departements/**", "/api/postes/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // Routes documents - CORRECTION IMPORTANTE
                        // Ne pas utiliser de double wildcard mal placé
                        .requestMatchers("/api/documents/download/**").authenticated()
                        .requestMatchers("/api/documents/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "LEAD", "STAFF", "STAGIAIRE", "SECRETAIRE")

                        .requestMatchers("/api/secretariat/**").hasRole("SECRETAIRE")
                        .requestMatchers("/api/factures/**", "/api/facturation-cycles/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/projets/dashboard/**",
                                "/api/projets/*/dashboard", "/api/projets/statistiques/**")
                                .hasAnyRole("ADMIN", "MANAGER", "STAFF", "STAGIAIRE", "LEAD")
                        .requestMatchers(HttpMethod.GET, "/api/projets/*/factures/**")
                                .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/projets/**")
                                .hasAnyRole("ADMIN", "MANAGER", "STAFF", "STAGIAIRE", "LEAD", "SECRETAIRE")
                        .requestMatchers("/api/projets/**")
                                .hasAnyRole("ADMIN", "MANAGER", "LEAD")
                        .requestMatchers("/api/taches/**")
                                .hasAnyRole("ADMIN", "MANAGER", "STAFF", "STAGIAIRE", "LEAD", "SECRETAIRE")
                        .requestMatchers("/api/evenements/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "STAFF", "STAGIAIRE", "LEAD", "SECRETAIRE")

                        // Autres routes
                        .requestMatchers(
                                "/api/projets/**",
                                "/api/taches/**",
                                "/api/evenements/**"
                        ).hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "STAFF")

                        .requestMatchers("/api/commentaires/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "STAFF", "STAGIAIRE")

                        .requestMatchers("/api/dashboard/**").authenticated()

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // Configuration Web MVC séparée - CORRIGÉE
    @Configuration
    public static class WebMvcConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            // Configuration correcte des ressources statiques
            registry.addResourceHandler("/static/**")
                    .addResourceLocations("classpath:/static/");

            // Pour les ressources publiques (optionnel)
            registry.addResourceHandler("/css/**")
                    .addResourceLocations("classpath:/static/css/");
            registry.addResourceHandler("/js/**")
                    .addResourceLocations("classpath:/static/js/");
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            // Ajoutez vos intercepteurs ici si nécessaire
            // Ne pas ajouter d'intercepteur qui pourrait causer des conflits
        }
    }
}
