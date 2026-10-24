package com.sfd.kyc.kyc.repo;

import com.sfd.kyc.kyc.domain.ResultatScoring;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultatScoringRepository extends JpaRepository<ResultatScoring, Long> {

    List<ResultatScoring> findByDossierIdOrderByCalculeLeDesc(Long dossierId);
}
