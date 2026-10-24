package com.sfd.kyc.auth.repo;

import com.sfd.kyc.auth.domain.TentativeConnexion;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TentativeConnexionRepository extends JpaRepository<TentativeConnexion, Long> {

    @Query("""
            select count(t) from TentativeConnexion t
            where t.email = :email and t.succes = false and t.horodatage >= :depuis
            """)
    long compterEchecsDepuis(@Param("email") String email, @Param("depuis") Instant depuis);
}
