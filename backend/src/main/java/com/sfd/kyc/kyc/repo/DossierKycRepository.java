package com.sfd.kyc.kyc.repo;

import com.sfd.kyc.kyc.domain.DossierKyc;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DossierKycRepository
        extends JpaRepository<DossierKyc, Long>, JpaSpecificationExecutor<DossierKyc> {

    @EntityGraph(attributePaths = {"client", "agence", "creePar", "soumisPar", "assigneA", "decidePar"})
    @Query("select d from DossierKyc d where d.id = :id")
    Optional<DossierKyc> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DossierKyc d where d.id = :id")
    Optional<DossierKyc> findPourMiseAJour(@Param("id") Long id);

    @Query("select coalesce(max(d.version), 0) from DossierKyc d where d.client.id = :clientId")
    int versionMax(@Param("clientId") Long clientId);
}
