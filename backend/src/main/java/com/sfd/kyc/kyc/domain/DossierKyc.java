package com.sfd.kyc.kyc.domain;

import com.sfd.kyc.auth.domain.Agence;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dossier_kyc")
@Getter
@Setter
@NoArgsConstructor
public class DossierKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    @Column(nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatutDossier statut = StatutDossier.BROUILLON;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_piece", nullable = false, length = 20)
    private TypePiece typePiece = TypePiece.CNI;

    @Column(name = "numero_piece", nullable = false, length = 40)
    private String numeroPiece;

    @Column(name = "date_expiration_piece", nullable = false)
    private LocalDate dateExpirationPiece;

    @Column(name = "revenu_mensuel", precision = 18, scale = 0)
    private BigDecimal revenuMensuel;

    @Column(name = "charges_mensuelles", precision = 18, scale = 0)
    private BigDecimal chargesMensuelles;

    @Column(name = "anciennete_activite_mois")
    private Integer ancienneteActiviteMois;

    @Column(name = "nb_personnes_charge")
    private Integer nbPersonnesCharge;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_risque", length = 10)
    private NiveauRisque niveauRisque;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par", nullable = false)
    private Utilisateur creePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soumis_par")
    private Utilisateur soumisPar;

    @Column(name = "soumis_le")
    private Instant soumisLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_a")
    private Utilisateur assigneA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decide_par")
    private Utilisateur decidePar;

    @Column(name = "decide_le")
    private Instant decideLe;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe = Instant.now();
}
