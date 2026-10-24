package com.sfd.kyc.common.security;

import com.sfd.kyc.auth.domain.Utilisateur;
import com.sfd.kyc.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey cle;
    private final String issuer;
    private final Duration accessTtl;
    private final Clock clock;

    public JwtService(AppProperties proprietes, Clock clock) {
        byte[] secret = Decoders.BASE64.decode(proprietes.jwt().secret());
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 octets encodés en base64.");
        }
        this.cle = Keys.hmacShaKeyFor(secret);
        this.issuer = proprietes.jwt().issuer();
        this.accessTtl = proprietes.jwt().accessTtl();
        this.clock = clock;
    }

    public String genererAccessToken(Utilisateur utilisateur) {
        Instant maintenant = clock.instant();
        return Jwts.builder()
                .issuer(issuer)
                .subject(utilisateur.getEmail())
                .claim("uid", utilisateur.getId())
                .claim("role", utilisateur.getRole().name())
                .claim("agenceId", utilisateur.getAgence() == null ? null : utilisateur.getAgence().getId())
                .claim("typ", "access")
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(accessTtl)))
                .signWith(cle, Jwts.SIG.HS256)
                .compact();
    }

    public Claims valider(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(cle)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("typ", String.class))) {
                throw new JwtException("Type de jeton inattendu");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Jeton invalide", e);
        }
    }

    public long accessTtlSecondes() {
        return accessTtl.toSeconds();
    }
}
