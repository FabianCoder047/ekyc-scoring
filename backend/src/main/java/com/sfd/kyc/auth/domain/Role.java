package com.sfd.kyc.auth.domain;

public enum Role {
    AGENT_ACCUEIL,
    ANALYSTE_KYC,
    SUPERVISEUR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
