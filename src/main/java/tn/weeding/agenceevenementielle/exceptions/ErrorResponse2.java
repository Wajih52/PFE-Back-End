package tn.weeding.agenceevenementielle.exceptions;


import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Structure de réponse d'erreur
 */
@Getter
public class ErrorResponse2 {
    // Getters
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public ErrorResponse2(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

}