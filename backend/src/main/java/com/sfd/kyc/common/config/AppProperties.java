package com.sfd.kyc.common.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Security security,
        Stockage stockage,
        Chiffrement chiffrement,
        Documents documents) {

    public record Jwt(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {

        public Jwt {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("JWT_SECRET est obligatoire (base64, 32 octets minimum).");
            }
            if (accessTtl == null) {
                accessTtl = Duration.ofMinutes(15);
            }
            if (refreshTtl == null) {
                refreshTtl = Duration.ofDays(7);
            }
            if (issuer == null || issuer.isBlank()) {
                issuer = "sfd-kyc";
            }
        }
    }

    public record Security(int maxTentatives, Duration fenetreTentatives, String corsOrigines) {

        public Security {
            if (maxTentatives <= 0) {
                maxTentatives = 5;
            }
            if (fenetreTentatives == null) {
                fenetreTentatives = Duration.ofMinutes(15);
            }
        }

        public List<String> originesAutorisees() {
            if (corsOrigines == null || corsOrigines.isBlank()) {
                return List.of("http://localhost:4200");
            }
            return Arrays.stream(corsOrigines.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    public record Stockage(String chemin) {

        public Stockage {
            if (chemin == null || chemin.isBlank()) {
                chemin = "./data/documents";
            }
        }
    }

    public record Chiffrement(String cle) {

        public Chiffrement {
            if (cle == null || cle.isBlank()) {
                throw new IllegalStateException("KYC_ENCRYPTION_KEY est obligatoire (base64, 32 octets).");
            }
        }
    }

    public record Documents(long tailleMaxOctets) {

        public Documents {
            if (tailleMaxOctets <= 0) {
                tailleMaxOctets = 5 * 1024 * 1024;
            }
        }
    }
}
