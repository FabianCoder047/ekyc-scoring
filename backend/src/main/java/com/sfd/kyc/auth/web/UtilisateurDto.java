package com.sfd.kyc.auth.web;

import com.sfd.kyc.auth.domain.Utilisateur;

public record UtilisateurDto(
        Long id,
        String email,
        String nom,
        String prenom,
        String role,
        Long agenceId,
        String agenceNom) {

    public static UtilisateurDto de(Utilisateur utilisateur) {
        return new UtilisateurDto(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getRole().name(),
                utilisateur.getAgence() == null ? null : utilisateur.getAgence().getId(),
                utilisateur.getAgence() == null ? null : utilisateur.getAgence().getNom());
    }
}
