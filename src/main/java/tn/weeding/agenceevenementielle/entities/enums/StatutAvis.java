package tn.weeding.agenceevenementielle.entities.enums;

import lombok.Getter;

@Getter
public enum StatutAvis {
    EN_ATTENTE("En attente de modération"),
    APPROUVE("Approuvé et visible"),
    REJETE("Rejeté par l'administrateur"),
    SIGNALE("Signalé par des utilisateurs");

    private final String description;

    StatutAvis(String description) {
        this.description = description;
    }

}