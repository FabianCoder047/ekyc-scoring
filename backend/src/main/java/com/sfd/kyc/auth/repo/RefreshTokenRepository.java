package com.sfd.kyc.auth.repo;

import com.sfd.kyc.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revoqueLe = :instant where r.utilisateur.id = :utilisateurId and r.revoqueLe is null")
    int revoquerTousActifs(@Param("utilisateurId") Long utilisateurId, @Param("instant") Instant instant);
}
