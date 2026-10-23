package com.example.backend_sirhgp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final Environment environment;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @Value("${jwt.secret:}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostConstruct
    void validateOrGenerateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            if (!environment.acceptsProfiles(Profiles.of("dev"))) {
                throw new IllegalStateException("JWT_SECRET est obligatoire hors du profil dev.");
            }

            byte[] randomSecret = new byte[32];
            new SecureRandom().nextBytes(randomSecret);
            secretKey = Base64.getEncoder().encodeToString(randomSecret);
            log.warn("JWT_SECRET absent : une clé temporaire aléatoire a été générée pour ce démarrage local. Les tokens expireront au redémarrage.");
            return;
        }

        final byte[] decodedSecret;
        try {
            decodedSecret = Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET doit être une valeur Base64 valide.", exception);
        }
        if (decodedSecret.length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 octets aléatoires une fois décodé.");
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> {
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> list) {
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        });
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
        claims.put("roles", roles);
        return generateToken(claims, userDetails);
    }

    public String generateImpersonationToken(UserDetails impersonator, UserDetails target, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", target.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .toList());
        claims.put("impersonator", impersonator.getUsername());
        claims.put("impersonatorId", ((com.example.backend_sirhgp.model.Employer) impersonator).getId());
        claims.put("cibleId", ((com.example.backend_sirhgp.model.Employer) target).getId());
        return buildToken(claims, target, expirationMs);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
