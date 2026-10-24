package com.sfd.kyc.admin.web;

import com.sfd.kyc.auth.domain.Agence;

public record AgenceDto(Long id, String code, String nom) {

    public static AgenceDto de(Agence agence) {
        return new AgenceDto(agence.getId(), agence.getCode(), agence.getNom());
    }
}
