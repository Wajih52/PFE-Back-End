package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.authentification.PasswordResetDto;
import tn.weeding.agenceevenementielle.dto.authentification.PasswordResetRequestDto;

public interface PasswordResetService {
void demanderReinitialisationMotDePasse(PasswordResetRequestDto request);
void reinitialiserMotDePasse(PasswordResetDto request);
}
