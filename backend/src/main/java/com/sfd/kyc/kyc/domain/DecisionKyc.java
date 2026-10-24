package com.sfd.kyc.kyc.domain;

import com.sfd.kyc.auth.domain.Utilisateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "decision_kyc")
@Getter
@Setter
@NoArgsConstructor
public class DecisionKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierKyc dossier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtapeDecision etape;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ValeurDecision decision;

    @Column(length = 500)
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decideur_id", nullable = false)
    private Utilisateur decideur;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe = Instant.now();
}
