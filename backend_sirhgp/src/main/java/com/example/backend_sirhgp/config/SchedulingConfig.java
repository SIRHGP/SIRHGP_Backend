package com.example.backend_sirhgp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CORRECTION : @EnableScheduling nécessaire pour que @Scheduled dans
 * TokenBlacklistService.cleanupExpiredTokens() s'exécute.
 * Sans cette configuration, le nettoyage automatique de la blacklist
 * est silencieusement ignoré → fuite mémoire progressive en production.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Classe de configuration pure — pas de beans à déclarer.
}