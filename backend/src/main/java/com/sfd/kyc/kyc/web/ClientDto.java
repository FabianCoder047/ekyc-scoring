package com.sfd.kyc.kyc.web;

import com.sfd.kyc.kyc.domain.Client;

public record ClientDto(Long id, String nom, String prenom, String telephone) {

    public static ClientDto de(Client client) {
        return new ClientDto(client.getId(), client.getNom(), client.getPrenom(), client.getTelephone());
    }
}
