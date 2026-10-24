package com.sfd.kyc.kyc.repo;

import com.sfd.kyc.kyc.domain.DecisionKyc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionKycRepository extends JpaRepository<DecisionKyc, Long> {

    List<DecisionKyc> findByDossierIdOrderByCreeLeAsc(Long dossierId);
}
