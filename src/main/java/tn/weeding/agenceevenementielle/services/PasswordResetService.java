package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.PasswordResetDto;
import tn.weeding.agenceevenementielle.dto.PasswordResetRequestDto;

public interface PasswordResetService {
void demanderReinitialisationMotDePasse(PasswordResetRequestDto request);
void reinitialiserMotDePasse(PasswordResetDto request);
}
