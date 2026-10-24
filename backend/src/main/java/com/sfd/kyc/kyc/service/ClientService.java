package com.sfd.kyc.kyc.service;

import com.sfd.kyc.common.security.CurrentUser;
import com.sfd.kyc.common.security.UtilisateurAuthentifie;
import com.sfd.kyc.kyc.repo.ClientRepository;
import com.sfd.kyc.kyc.web.ClientDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liste des clients de l'agence de l'agent, nécessaire au formulaire de
 * création de dossier (endpoint de lecture ajouté au périmètre de l'API).
 */
@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @PreAuthorize("hasRole('AGENT_ACCUEIL')")
    @Transactional(readOnly = true)
    public List<ClientDto> listerPourMonAgence() {
        UtilisateurAuthentifie agent = CurrentUser.get();
        if (agent.agenceId() == null) {
            return List.of();
        }
        return clientRepository.findByAgenceIdOrderByNomAscPrenomAsc(agent.agenceId()).stream()
                .map(ClientDto::de)
                .toList();
    }
}
