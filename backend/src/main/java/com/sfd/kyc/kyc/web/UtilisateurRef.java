package com.sfd.kyc.kyc.web;

import com.sfd.kyc.auth.domain.Utilisateur;

public record UtilisateurRef(Long id, String nom, String prenom) {

    public static UtilisateurRef de(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        return new UtilisateurRef(utilisateur.getId(), utilisateur.getNom(), utilisateur.getPrenom());
    }
}
