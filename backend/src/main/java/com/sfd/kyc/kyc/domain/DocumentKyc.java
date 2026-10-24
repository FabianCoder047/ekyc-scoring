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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_kyc")
@Getter
@Setter
@NoArgsConstructor
public class DocumentKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierKyc dossier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TypeDocument type;

    @Column(name = "nom_original", nullable = false, length = 200)
    private String nomOriginal;

    @Column(name = "cle_stockage", nullable = false, unique = true, length = 80)
    private String cleStockage;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "taille_octets", nullable = false)
    private long tailleOctets;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] iv;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depose_par", nullable = false)
    private Utilisateur deposePar;

    @Column(name = "depose_le", nullable = false)
    private Instant deposeLe = Instant.now();

    @Column(name = "remplace_par")
    private Long remplacePar;
}
