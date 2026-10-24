package com.sfd.kyc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sfd.kyc.auth.domain.Agence;
import com.sfd.kyc.auth.domain.Role;
import com.sfd.kyc.common.security.UtilisateurAuthentifie;
import com.sfd.kyc.kyc.domain.Client;
import com.sfd.kyc.kyc.repo.ClientRepository;
import com.sfd.kyc.kyc.service.ClientService;
import com.sfd.kyc.kyc.web.ClientDto;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test unitaire Mockito du cloisonnement par agence : le service ne doit
 * interroger que les clients de l'agence de l'agent connecté.
 */
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void liste_uniquement_les_clients_de_l_agence_de_l_agent() {
        authentifier(new UtilisateurAuthentifie(7L, "agent@sfd.tg", Role.AGENT_ACCUEIL, 42L));

        Client client = new Client();
        client.setId(3L);
        client.setNom("ADJOVI");
        client.setPrenom("Kossi");
        client.setTelephone("+22890000002");
        Agence agence = new Agence();
        agence.setId(42L);
        client.setAgence(agence);

        when(clientRepository.findByAgenceIdOrderByNomAscPrenomAsc(42L))
                .thenReturn(List.of(client));

        List<ClientDto> resultat = clientService.listerPourMonAgence();

        assertThat(resultat)
                .containsExactly(new ClientDto(3L, "ADJOVI", "Kossi", "+22890000002"));
        verify(clientRepository).findByAgenceIdOrderByNomAscPrenomAsc(42L);
    }

    @Test
    void retourne_une_liste_vide_si_l_agent_n_a_pas_d_agence() {
        authentifier(new UtilisateurAuthentifie(8L, "sans-agence@sfd.tg", Role.AGENT_ACCUEIL, null));

        assertThat(clientService.listerPourMonAgence()).isEmpty();
        verifyNoInteractions(clientRepository);
    }

    private void authentifier(UtilisateurAuthentifie utilisateur) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                utilisateur,
                null,
                List.of(new SimpleGrantedAuthority(utilisateur.role().authority()))));
    }
}
