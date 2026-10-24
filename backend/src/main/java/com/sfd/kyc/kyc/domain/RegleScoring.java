package com.sfd.kyc.kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "critere_scoring")
@Getter
@Setter
@NoArgsConstructor
public class RegleScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CritereScoring critere;

    @Column(name = "borne_min", precision = 18, scale = 4)
    private BigDecimal borneMin;

    @Column(name = "borne_max", precision = 18, scale = 4)
    private BigDecimal borneMax;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false, length = 120)
    private String libelle;

    @Column(nullable = false)
    private boolean actif = true;
}
