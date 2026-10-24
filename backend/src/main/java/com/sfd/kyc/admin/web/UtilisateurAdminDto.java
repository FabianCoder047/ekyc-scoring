package com.sfd.kyc.admin.web;

import com.sfd.kyc.auth.domain.Utilisateur;
import java.time.Instant;

public record UtilisateurAdminDto(
        Long id,
        String email,
        String nom,
        String prenom,
        String role,
        Long agenceId,
        String agenceNom,
        boolean actif,
        Instant creeLe) {

    public static UtilisateurAdminDto de(Utilisateur utilisateur) {
        return new UtilisateurAdminDto(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getRole().name(),
                utilisateur.getAgence() == null ? null : utilisateur.getAgence().getId(),
                utilisateur.getAgence() == null ? null : utilisateur.getAgence().getNom(),
                utilisateur.isActif(),
                utilisateur.getCreeLe());
    }
}
