package com.sfd.kyc.admin.web;

import com.sfd.kyc.auth.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UtilisateurModificationRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100)
        String prenom,

        @NotNull(message = "Le rôle est obligatoire")
        Role role,

        Long agenceId,

        boolean actif,

        @Size(min = 10, max = 100, message = "Le mot de passe doit contenir au moins 10 caractères")
        String nouveauMotDePasse) {
}
