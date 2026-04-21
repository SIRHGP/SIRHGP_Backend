package com.example.backend_sirhgp.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de blacklist de tokens JWT (in-memory).
 *
 * CORRECTION : @EnableScheduling doit être ajouté dans une classe @Configuration
 * (ex: SchedulingConfig ou directement dans la classe principale) pour que
 * @Scheduled soit effectif. Sans ça, cleanupExpiredTokens() ne s'exécute jamais.
 *
 * Exemple à ajouter dans un fichier SchedulingConfig.java :
 *
 *   @Configuration
 *   @EnableScheduling
 *   public class SchedulingConfig {}
 *
 * ⚠️  Limitation in-memory documentée :
 * La blacklist est perdue au redémarrage. Les tokens révoqués avant un redémarrage
 * redeviennent temporairement valides jusqu'à leur expiration naturelle.
 * Pour la production : migrer vers Redis (RedisTemplate avec TTL = durée du JWT).
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    // token → date d'expiration du JWT (pour le nettoyage automatique)
    private final ConcurrentHashMap<String, Date> blacklist = new ConcurrentHashMap<>();

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Ajoute un token à la blacklist.
     */
    public void blacklistToken(String token) {
        try {
            Date expiration = extractExpiration(token);
            blacklist.put(token, expiration);
            log.debug("Token blacklisté (expire le {})", expiration);
        } catch (Exception e) {
            // Token malformé : blacklisté avec une durée de sécurité de 24h
            blacklist.put(token, new Date(System.currentTimeMillis() + 86_400_000L));
            log.warn("Token malformé blacklisté avec durée de sécurité de 24h");
        }
    }

    /**
     * Vérifie si un token est blacklisté.
     */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Nettoyage automatique des tokens expirés toutes les heures.
     * CORRECTION : nécessite @EnableScheduling dans une @Configuration pour fonctionner.
     */
    @Scheduled(fixedRate = 3_600_000)
    public void cleanupExpiredTokens() {
        Date now = new Date();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.info("Blacklist : {} tokens expirés supprimés ({} restants)", removed, blacklist.size());
        }
    }

    private Date extractExpiration(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }
}