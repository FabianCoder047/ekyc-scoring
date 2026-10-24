package com.sfd.kyc.kyc.repo;

import com.sfd.kyc.kyc.domain.DocumentKyc;
import com.sfd.kyc.kyc.domain.TypeDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentKycRepository extends JpaRepository<DocumentKyc, Long> {

    List<DocumentKyc> findByDossierIdOrderByDeposeLeAsc(Long dossierId);

    List<DocumentKyc> findByDossierIdAndRemplaceParIsNullOrderByDeposeLeAsc(Long dossierId);

    Optional<DocumentKyc> findFirstByDossierIdAndTypeAndRemplaceParIsNullOrderByDeposeLeDesc(
            Long dossierId, TypeDocument type);

    Optional<DocumentKyc> findByIdAndDossierId(Long id, Long dossierId);

    boolean existsByDossierIdAndTypeAndRemplaceParIsNull(Long dossierId, TypeDocument type);
}
