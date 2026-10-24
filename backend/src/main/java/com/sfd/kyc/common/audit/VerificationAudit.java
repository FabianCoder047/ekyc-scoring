package com.sfd.kyc.common.audit;

public record VerificationAudit(boolean valide, Long premiereLigneInvalide, long lignesVerifiees) {
}
