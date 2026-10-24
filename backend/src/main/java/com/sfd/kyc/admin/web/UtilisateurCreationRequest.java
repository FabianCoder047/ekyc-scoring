package com.sfd.kyc.admin.web;

import com.sfd.kyc.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UtilisateurCreationRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 10, max = 100, message = "Le mot de passe doit contenir au moins 10 caractères")
        String motDePasse,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100)
        String prenom,

        @NotNull(message = "Le rôle est obligatoire")
        Role role,

        Long agenceId) {
}
