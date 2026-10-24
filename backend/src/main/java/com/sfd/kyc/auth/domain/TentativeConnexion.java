package com.sfd.kyc.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tentative_connexion")
@Getter
@Setter
@NoArgsConstructor
public class TentativeConnexion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Column(nullable = false)
    private boolean succes;

    @Column(nullable = false)
    private Instant horodatage = Instant.now();
}
