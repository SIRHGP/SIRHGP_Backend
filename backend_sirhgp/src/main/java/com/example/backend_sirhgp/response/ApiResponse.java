// ─── ApiResponse.java ────────────────────────────────────────────────────────
package com.example.backend_sirhgp.response;

import lombok.Data;

/**
 * Enveloppe standard pour toutes les réponses API.
 * Pas de modification — déjà correct dans l'original.
 */
@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}