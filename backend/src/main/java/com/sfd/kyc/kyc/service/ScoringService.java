package com.sfd.kyc.kyc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfd.kyc.auth.repo.UtilisateurRepository;
import com.sfd.kyc.common.audit.AuditAction;
import com.sfd.kyc.common.audit.AuditService;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import com.sfd.kyc.common.security.CurrentUser;
import com.sfd.kyc.common.security.UtilisateurAuthentifie;
import com.sfd.kyc.kyc.domain.CritereScoring;
import com.sfd.kyc.kyc.domain.DecisionKyc;
import com.sfd.kyc.kyc.domain.DossierKyc;
import com.sfd.kyc.kyc.domain.EtapeDecision;
import com.sfd.kyc.kyc.domain.ResultatScoring;
import com.sfd.kyc.kyc.domain.StatutDossier;
import com.sfd.kyc.kyc.domain.ValeurDecision;
import com.sfd.kyc.kyc.repo.DecisionKycRepository;
import com.sfd.kyc.kyc.repo.DossierKycRepository;
import com.sfd.kyc.kyc.repo.RegleScoringRepository;
import com.sfd.kyc.kyc.repo.ResultatScoringRepository;
import com.sfd.kyc.kyc.web.ResultatScoringDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoringService {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final DossierKycRepository dossierRepository;
    private final RegleScoringRepository regleRepository;
    private final ResultatScoringRepository resultatRepository;
    private final DecisionKycRepository decisionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ScoringCalculator calculator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public ScoringService(
            DossierKycRepository dossierRepository,
            RegleScoringRepository regleRepository,
            ResultatScoringRepository resultatRepository,
            DecisionKycRepository decisionRepository,
            UtilisateurRepository utilisateurRepository,
            ScoringCalculator calculator,
            AuditService auditService,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            Clock clock) {
        this.dossierRepository = dossierRepository;
        this.regleRepository = regleRepository;
        this.resultatRepository = resultatRepository;
        this.decisionRepository = decisionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.calculator = calculator;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ANALYSTE_KYC')")
    @Transactional
    public ResultatScoringDto calculer(Long dossierId) {
        UtilisateurAuthentifie analyste = CurrentUser.get();
        DossierKyc dossier = dossierRepository.findPourMiseAJour(dossierId)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));

        if (dossier.getStatut() != StatutDossier.EN_REVUE) {
            throw ApiException.transition("Le scoring n'est possible que sur un dossier en revue.");
        }
        if (dossier.getAssigneA() == null || !dossier.getAssigneA().getId().equals(analyste.id())) {
            auditService.accesRefuse("SCORING_CALCULE", "DOSSIER_KYC", dossierId);
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCES_REFUSE, "Accès refusé.");
        }

        verifierDonneesFinancieres(dossier);

        BigDecimal ratio = dossier.getChargesMensuelles()
                .multiply(CENT)
                .divide(dossier.getRevenuMensuel(), 4, RoundingMode.HALF_UP);

        Map<CritereScoring, BigDecimal> valeurs = new EnumMap<>(CritereScoring.class);
        valeurs.put(CritereScoring.RATIO_ENDETTEMENT, ratio);
        valeurs.put(CritereScoring.ANCIENNETE_ACTIVITE,
                BigDecimal.valueOf(dossier.getAncienneteActiviteMois()));
        valeurs.put(CritereScoring.REVENU_MENSUEL, dossier.getRevenuMensuel());

        Map<CritereScoring, Boolean> neutres = ScoringCalculator.neutresVides();
        Historique historique = historiqueRemboursement(dossier.getClient().getId());
        if (historique.total() == 0) {
            neutres.put(CritereScoring.HISTORIQUE_REMBOURSEMENT, true);
        } else {
            BigDecimal pourcentage = BigDecimal.valueOf(historique.retards())
                    .multiply(CENT)
                    .divide(BigDecimal.valueOf(historique.total()), 4, RoundingMode.HALF_UP);
            valeurs.put(CritereScoring.HISTORIQUE_REMBOURSEMENT, pourcentage);
        }

        ScoringCalculator.ResultatCalcul calcul = calculator.calculer(
                valeurs, neutres, regleRepository.findByActifTrueOrderByCritereAscBorneMinAsc());

        Instant maintenant = clock.instant();
        ResultatScoring resultat = new ResultatScoring();
        resultat.setDossier(dossier);
        resultat.setScore(calcul.score());
        resultat.setPalier(calcul.palier());
        resultat.setDetail(serialiser(calcul.detail()));
        resultat.setCalculePar(utilisateurRepository.getReferenceById(analyste.id()));
        resultat.setCalculeLe(maintenant);
        resultat = resultatRepository.saveAndFlush(resultat);

        DecisionKyc analyse = new DecisionKyc();
        analyse.setDossier(dossier);
        analyse.setEtape(EtapeDecision.ANALYSE);
        analyse.setDecision(ValeurDecision.RECOMMANDE);
        analyse.setCommentaire("Score " + calcul.score() + "/100 - " + calcul.palier().name());
        analyse.setDecideur(utilisateurRepository.getReferenceById(analyste.id()));
        analyse.setCreeLe(maintenant);
        decisionRepository.save(analyse);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("dossierId", dossierId);
        details.put("score", calcul.score());
        details.put("palier", calcul.palier().name());
        auditService.journaliser(
                AuditAction.SCORING_CALCULE, "RESULTAT_SCORING", resultat.getId(), null, details);

        return ResultatScoringDto.de(resultat, objectMapper);
    }

    private void verifierDonneesFinancieres(DossierKyc dossier) {
        List<String> manquants = new ArrayList<>();
        if (dossier.getRevenuMensuel() == null || dossier.getRevenuMensuel().signum() <= 0) {
            manquants.add("revenuMensuel");
        }
        if (dossier.getChargesMensuelles() == null) {
            manquants.add("chargesMensuelles");
        }
        if (dossier.getAncienneteActiviteMois() == null) {
            manquants.add("ancienneteActiviteMois");
        }
        if (!manquants.isEmpty()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorCodes.SCORING_DONNEES_INCOMPLETES,
                    "Les données financières du dossier sont incomplètes : " + String.join(", ", manquants),
                    Map.of("champs", manquants));
        }
    }

    private Historique historiqueRemboursement(Long clientId) {
        LocalDate aujourdHui = LocalDate.now(clock);
        return jdbcTemplate.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (
                         WHERE (e.date_paiement IS NOT NULL AND e.date_paiement > e.date_echeance)
                            OR (e.date_paiement IS NULL AND e.date_echeance < ?)
                       ) AS retards
                FROM echeance e
                JOIN credit c ON c.id = e.credit_id
                WHERE c.client_id = ? AND e.date_echeance <= ?
                """,
                (rs, rowNum) -> new Historique(rs.getLong("total"), rs.getLong("retards")),
                aujourdHui,
                clientId,
                aujourdHui);
    }

    private String serialiser(List<ScoringCalculator.DetailCritere> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation du détail de scoring impossible", e);
        }
    }

    private record Historique(long total, long retards) {
    }
}
