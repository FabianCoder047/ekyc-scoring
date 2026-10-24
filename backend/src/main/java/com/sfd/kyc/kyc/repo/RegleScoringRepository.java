package com.sfd.kyc.kyc.repo;

import com.sfd.kyc.kyc.domain.CritereScoring;
import com.sfd.kyc.kyc.domain.RegleScoring;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegleScoringRepository extends JpaRepository<RegleScoring, Long> {

    List<RegleScoring> findByActifTrueOrderByCritereAscBorneMinAsc();

    List<RegleScoring> findByCritereAndActifTrueOrderByBorneMinAsc(CritereScoring critere);

    List<RegleScoring> findByCritereOrderByBorneMinAsc(CritereScoring critere);
}
