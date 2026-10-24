package com.sfd.kyc.kyc.web;

import com.sfd.kyc.kyc.domain.DecisionKyc;
import java.time.Instant;

public record DecisionKycDto(
        Long id,
        String etape,
        String decision,
        String commentaire,
        UtilisateurRef decideur,
        Instant creeLe) {

    public static DecisionKycDto de(DecisionKyc decision) {
        return new DecisionKycDto(
                decision.getId(),
                decision.getEtape().name(),
                decision.getDecision().name(),
                decision.getCommentaire(),
                UtilisateurRef.de(decision.getDecideur()),
                decision.getCreeLe());
    }
}
