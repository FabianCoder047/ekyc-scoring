package com.sfd.kyc.kyc.web;

import java.util.List;

public record HistoriqueDto(
        Long dossierId,
        List<DecisionKycDto> decisions,
        List<ResultatScoringDto> scorings) {
}
