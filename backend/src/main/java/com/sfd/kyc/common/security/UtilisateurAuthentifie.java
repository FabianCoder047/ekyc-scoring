package com.sfd.kyc.common.security;

import com.sfd.kyc.auth.domain.Role;

public record UtilisateurAuthentifie(Long id, String email, Role role, Long agenceId) {
}
