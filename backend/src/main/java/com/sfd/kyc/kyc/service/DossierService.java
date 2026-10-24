package com.sfd.kyc.kyc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfd.kyc.auth.domain.Role;
import com.sfd.kyc.auth.repo.UtilisateurRepository;
import com.sfd.kyc.common.audit.AuditAction;
import com.sfd.kyc.common.audit.AuditService;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import com.sfd.kyc.common.security.CurrentUser;
import com.sfd.kyc.common.security.UtilisateurAuthentifie;
import com.sfd.kyc.common.web.PageResponse;
import com.sfd.kyc.kyc.domain.Client;
import com.sfd.kyc.kyc.domain.DecisionKyc;
import com.sfd.kyc.kyc.domain.DossierKyc;
import com.sfd.kyc.kyc.domain.EtapeDecision;
import com.sfd.kyc.kyc.domain.StatutDossier;
import com.sfd.kyc.kyc.domain.TypeDocument;
import com.sfd.kyc.kyc.domain.TypePiece;
import com.sfd.kyc.kyc.domain.ValeurDecision;
import com.sfd.kyc.kyc.repo.ClientRepository;
import com.sfd.kyc.kyc.repo.DecisionKycRepository;
import com.sfd.kyc.kyc.repo.DocumentKycRepository;
import com.sfd.kyc.kyc.repo.DossierKycRepository;
import com.sfd.kyc.kyc.repo.ResultatScoringRepository;
import com.sfd.kyc.kyc.web.DecisionKycDto;
import com.sfd.kyc.kyc.web.DecisionRequest;
import com.sfd.kyc.kyc.web.DocumentDto;
import com.sfd.kyc.kyc.web.DossierCreationRequest;
import com.sfd.kyc.kyc.web.DossierDetail;
import com.sfd.kyc.kyc.web.DossierModificationRequest;
import com.sfd.kyc.kyc.web.DossierResume;
import com.sfd.kyc.kyc.web.HistoriqueDto;
import com.sfd.kyc.kyc.web.ResultatScoringDto;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DossierService {

    private final DossierKycRepository dossierRepository;
    private final DocumentKycRepository documentRepository;
    private final ClientRepository clientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DecisionKycRepository decisionRepository;
    private final ResultatScoringRepository resultatRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DossierService(
            DossierKycRepository dossierRepository,
            DocumentKycRepository documentRepository,
            ClientRepository clientRepository,
            UtilisateurRepository utilisateurRepository,
            DecisionKycRepository decisionRepository,
            ResultatScoringRepository resultatRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.dossierRepository = dossierRepository;
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.decisionRepository = decisionRepository;
        this.resultatRepository = resultatRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('AGENT_ACCUEIL')")
    @Transactional
    public DossierDetail creer(DossierCreationRequest demande) {
        UtilisateurAuthentifie agent = CurrentUser.get();
        Client client = clientRepository.findById(demande.clientId())
                .orElseThrow(() -> ApiException.introuvable("Client introuvable."));
        if (agent.agenceId() == null || !client.getAgence().getId().equals(agent.agenceId())) {
            auditService.accesRefuse("DOSSIER_CREE", "CLIENT", client.getId());
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCES_REFUSE, "Accès refusé.");
        }

        DossierKyc dossier = new DossierKyc();
        dossier.setClient(client);
        dossier.setAgence(client.getAgence());
        dossier.setVersion(dossierRepository.versionMax(client.getId()) + 1);
        dossier.setCreePar(utilisateurRepository.getReferenceById(agent.id()));
        dossier.setCreeLe(clock.instant());
        appliquerChamps(dossier, demande.typePiece(), demande.numeroPiece(), demande.dateExpirationPiece(),
                demande.revenuMensuel(), demande.chargesMensuelles(),
                demande.ancienneteActiviteMois(), demande.nbPersonnesCharge());
        dossier = dossierRepository.saveAndFlush(dossier);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("clientId", client.getId());
        details.put("version", dossier.getVersion());
        details.put("statut", dossier.getStatut().name());
        auditService.journaliser(AuditAction.DOSSIER_CREE, "DOSSIER_KYC", dossier.getId(), null, details);

        return detail(dossier, false, List.of());
    }

    /**
     * Modification d'un brouillon. Sur un dossier rejeté, la correction crée une
     * nouvelle version (le dossier rejeté reste consultable).
     */
    @PreAuthorize("hasRole('AGENT_ACCUEIL')")
    @Transactional
    public DossierDetail modifier(Long id, DossierModificationRequest demande) {
        UtilisateurAuthentifie agent = CurrentUser.get();
        DossierKyc existant = dossierRepository.findPourMiseAJour(id)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));
        verifierAgentProprietaire(existant, agent, "DOSSIER_MODIFIE");

        if (existant.getStatut() == StatutDossier.BROUILLON) {
            appliquerChamps(existant, demande.typePiece(), demande.numeroPiece(), demande.dateExpirationPiece(),
                    demande.revenuMensuel(), demande.chargesMensuelles(),
                    demande.ancienneteActiviteMois(), demande.nbPersonnesCharge());
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("statut", existant.getStatut().name());
            details.put("version", existant.getVersion());
            auditService.journaliser(
                    AuditAction.DOSSIER_MODIFIE, "DOSSIER_KYC", existant.getId(), null, details);
            return detail(existant, false, documents(existant.getId()));
        }

        if (existant.getStatut() == StatutDossier.REJETE) {
            DossierKyc nouvelleVersion = new DossierKyc();
            nouvelleVersion.setClient(existant.getClient());
            nouvelleVersion.setAgence(existant.getAgence());
            nouvelleVersion.setVersion(dossierRepository.versionMax(existant.getClient().getId()) + 1);
            nouvelleVersion.setCreePar(utilisateurRepository.getReferenceById(agent.id()));
            nouvelleVersion.setCreeLe(clock.instant());
            appliquerChamps(nouvelleVersion, demande.typePiece(), demande.numeroPiece(),
                    demande.dateExpirationPiece(), demande.revenuMensuel(), demande.chargesMensuelles(),
                    demande.ancienneteActiviteMois(), demande.nbPersonnesCharge());
            nouvelleVersion = dossierRepository.saveAndFlush(nouvelleVersion);

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("dossierPrecedentId", existant.getId());
            details.put("version", nouvelleVersion.getVersion());
            details.put("statut", nouvelleVersion.getStatut().name());
            auditService.journaliser(
                    AuditAction.DOSSIER_MODIFIE, "DOSSIER_KYC", nouvelleVersion.getId(), null, details);
            return detail(nouvelleVersion, false, List.of());
        }

        throw ApiException.transition("Seul un dossier brouillon ou rejeté peut être modifié.");
    }

    @PreAuthorize("hasRole('AGENT_ACCUEIL')")
    @Transactional
    public DossierDetail soumettre(Long id) {
        UtilisateurAuthentifie agent = CurrentUser.get();
        DossierKyc dossier = dossierRepository.findPourMiseAJour(id)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));
        verifierAgentProprietaire(dossier, agent, "DOSSIER_SOUMIS");

        if (dossier.getStatut() != StatutDossier.BROUILLON) {
            throw ApiException.transition("Seul un dossier brouillon peut être soumis.");
        }
        if (dossier.getNumeroPiece() == null || dossier.getNumeroPiece().isBlank()
                || dossier.getDateExpirationPiece() == null) {
            throw ApiException.validation("Les champs obligatoires du dossier sont incomplets.");
        }
        if (dossier.getDateExpirationPiece().isBefore(LocalDate.now(clock))) {
            throw new ApiException(
                    HttpStatus.CONFLICT, ErrorCodes.PIECE_EXPIREE, "La pièce d'identité est expirée.");
        }
        boolean recto = documentRepository.existsByDossierIdAndTypeAndRemplaceParIsNull(
                id, TypeDocument.RECTO_PIECE);
        boolean verso = documentRepository.existsByDossierIdAndTypeAndRemplaceParIsNull(
                id, TypeDocument.VERSO_PIECE);
        if (!recto || !verso) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.DOCUMENT_MANQUANT,
                    "Le recto et le verso de la pièce d'identité sont obligatoires.");
        }

        dossier.setStatut(StatutDossier.SOUMIS);
        dossier.setSoumisPar(utilisateurRepository.getReferenceById(agent.id()));
        dossier.setSoumisLe(clock.instant());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("statut", dossier.getStatut().name());
        details.put("version", dossier.getVersion());
        auditService.journaliser(AuditAction.DOSSIER_SOUMIS, "DOSSIER_KYC", dossier.getId(), null, details);

        return detail(dossier, false, documents(id));
    }

    @PreAuthorize("hasRole('ANALYSTE_KYC')")
    @Transactional
    public DossierDetail prendreEnCharge(Long id) {
        UtilisateurAuthentifie analyste = CurrentUser.get();
        DossierKyc dossier = dossierRepository.findPourMiseAJour(id)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));
        if (dossier.getStatut() != StatutDossier.SOUMIS) {
            throw ApiException.transition("Seul un dossier soumis peut être pris en charge.");
        }

        dossier.setStatut(StatutDossier.EN_REVUE);
        dossier.setAssigneA(utilisateurRepository.getReferenceById(analyste.id()));

        DecisionKyc decision = new DecisionKyc();
        decision.setDossier(dossier);
        decision.setEtape(EtapeDecision.PRISE_EN_CHARGE);
        decision.setDecision(ValeurDecision.ASSIGNE);
        decision.setCommentaire("Prise en charge du dossier");
        decision.setDecideur(utilisateurRepository.getReferenceById(analyste.id()));
        decision.setCreeLe(clock.instant());
        decisionRepository.save(decision);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("statut", dossier.getStatut().name());
        details.put("analysteId", analyste.id());
        auditService.journaliser(AuditAction.DOSSIER_ASSIGNE, "DOSSIER_KYC", dossier.getId(), null, details);

        return detail(dossier, true, documents(id));
    }

    @PreAuthorize("hasRole('SUPERVISEUR')")
    @Transactional
    public DossierDetail decider(Long id, DecisionRequest demande) {
        UtilisateurAuthentifie superviseur = CurrentUser.get();
        DossierKyc dossier = dossierRepository.findPourMiseAJour(id)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));

        if (dossier.getStatut() != StatutDossier.EN_REVUE) {
            throw ApiException.transition("Seul un dossier en revue peut recevoir une décision finale.");
        }

        boolean aSoumis = dossier.getSoumisPar() != null
                && dossier.getSoumisPar().getId().equals(superviseur.id());
        boolean aPrisEnCharge = dossier.getAssigneA() != null
                && dossier.getAssigneA().getId().equals(superviseur.id());
        if (aSoumis || aPrisEnCharge) {
            auditService.accesRefuse("DOSSIER_DECISION", "DOSSIER_KYC", dossier.getId());
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.QUATRE_YEUX,
                    "Le principe des quatre yeux interdit de valider un dossier que vous avez traité.");
        }

        ValeurDecision decision;
        try {
            decision = ValeurDecision.valueOf(demande.decision().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("La décision doit être VALIDE ou REJETE.");
        }
        if (decision != ValeurDecision.VALIDE && decision != ValeurDecision.REJETE) {
            throw ApiException.validation("La décision doit être VALIDE ou REJETE.");
        }

        String motif = demande.motif() == null ? null : demande.motif().trim();
        if (decision == ValeurDecision.REJETE && (motif == null || motif.isBlank())) {
            throw ApiException.validation("Un motif est obligatoire en cas de rejet.");
        }
        if (motif != null && motif.length() > 500) {
            throw ApiException.validation("Le motif ne doit pas dépasser 500 caractères.");
        }

        Instant maintenant = clock.instant();
        dossier.setStatut(decision == ValeurDecision.VALIDE ? StatutDossier.VALIDE : StatutDossier.REJETE);
        dossier.setDecidePar(utilisateurRepository.getReferenceById(superviseur.id()));
        dossier.setDecideLe(maintenant);
        dossier.setMotifRejet(decision == ValeurDecision.REJETE ? motif : null);

        DecisionKyc ligne = new DecisionKyc();
        ligne.setDossier(dossier);
        ligne.setEtape(EtapeDecision.VALIDATION);
        ligne.setDecision(decision);
        ligne.setCommentaire(motif);
        ligne.setDecideur(utilisateurRepository.getReferenceById(superviseur.id()));
        ligne.setCreeLe(maintenant);
        decisionRepository.save(ligne);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("statut", dossier.getStatut().name());
        details.put("decision", decision.name());
        auditService.journaliser(
                decision == ValeurDecision.VALIDE ? AuditAction.DOSSIER_VALIDE : AuditAction.DOSSIER_REJETE,
                "DOSSIER_KYC",
                dossier.getId(),
                null,
                details);

        return detail(dossier, true, documents(id));
    }

    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL','ANALYSTE_KYC','SUPERVISEUR')")
    @Transactional(readOnly = true)
    public PageResponse<DossierResume> lister(StatutDossier statut, String q, int page, int taille) {
        UtilisateurAuthentifie utilisateur = CurrentUser.get();
        int pageSure = Math.max(page, 0);
        int tailleSure = Math.min(Math.max(taille, 1), 100);

        if (utilisateur.role() == Role.AGENT_ACCUEIL && utilisateur.agenceId() == null) {
            return PageResponse.de(List.of(), pageSure, tailleSure, 0);
        }

        Specification<DossierKyc> specification = (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (utilisateur.role() == Role.AGENT_ACCUEIL) {
                predicats.add(cb.equal(root.get("agence").get("id"), utilisateur.agenceId()));
            }
            if (statut != null) {
                predicats.add(cb.equal(root.get("statut"), statut));
            }
            if (q != null && !q.isBlank()) {
                String motif = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                List<Predicate> recherche = new ArrayList<>();
                recherche.add(cb.like(cb.lower(root.get("client").get("nom")), motif));
                recherche.add(cb.like(cb.lower(root.get("client").get("prenom")), motif));
                try {
                    Long identifiant = Long.valueOf(q.trim());
                    recherche.add(cb.equal(root.get("id"), identifiant));
                } catch (NumberFormatException ignored) {
                    // La recherche textuelle reste valable.
                }
                predicats.add(cb.or(recherche.toArray(Predicate[]::new)));
            }
            return cb.and(predicats.toArray(Predicate[]::new));
        };

        Page<DossierKyc> resultat = dossierRepository.findAll(
                specification,
                PageRequest.of(pageSure, tailleSure, Sort.by(Sort.Direction.DESC, "creeLe")));

        List<DossierResume> contenu = resultat.getContent().stream()
                .map(DossierResume::de)
                .toList();
        return PageResponse.de(contenu, pageSure, tailleSure, resultat.getTotalElements());
    }

    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL','ANALYSTE_KYC','SUPERVISEUR')")
    @Transactional(readOnly = true)
    public DossierDetail consulter(Long id) {
        UtilisateurAuthentifie utilisateur = CurrentUser.get();
        DossierKyc dossier = dossierRepository.findDetailById(id)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));

        if (utilisateur.role() == Role.AGENT_ACCUEIL
                && (utilisateur.agenceId() == null
                        || !dossier.getAgence().getId().equals(utilisateur.agenceId()))) {
            auditService.accesRefuse("DOSSIER_CONSULTE", "DOSSIER_KYC", id);
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCES_REFUSE, "Accès refusé.");
        }

        boolean numeroEnClair = utilisateur.role() == Role.ANALYSTE_KYC
                || utilisateur.role() == Role.SUPERVISEUR;
        return detail(dossier, numeroEnClair, documents(id));
    }

    @PreAuthorize("hasAnyRole('ANALYSTE_KYC','SUPERVISEUR')")
    @Transactional(readOnly = true)
    public HistoriqueDto historique(Long id) {
        dossierRepository.findById(id)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));

        List<DecisionKycDto> decisions = decisionRepository.findByDossierIdOrderByCreeLeAsc(id).stream()
                .map(DecisionKycDto::de)
                .toList();
        List<ResultatScoringDto> scorings = resultatRepository.findByDossierIdOrderByCalculeLeDesc(id).stream()
                .map(resultat -> ResultatScoringDto.de(resultat, objectMapper))
                .toList();
        return new HistoriqueDto(id, decisions, scorings);
    }

    private DossierDetail detail(DossierKyc dossier, boolean numeroEnClair, List<DocumentDto> documents) {
        return DossierDetail.de(dossier, numeroEnClair, documents);
    }

    private List<DocumentDto> documents(Long dossierId) {
        return documentRepository.findByDossierIdOrderByDeposeLeAsc(dossierId).stream()
                .map(DocumentDto::de)
                .toList();
    }

    private void appliquerChamps(
            DossierKyc dossier,
            TypePiece typePiece,
            String numeroPiece,
            LocalDate dateExpiration,
            java.math.BigDecimal revenu,
            java.math.BigDecimal charges,
            Integer anciennete,
            Integer personnesCharge) {
        dossier.setTypePiece(typePiece == null ? TypePiece.CNI : typePiece);
        dossier.setNumeroPiece(numeroPiece.trim());
        dossier.setDateExpirationPiece(dateExpiration);
        dossier.setRevenuMensuel(revenu);
        dossier.setChargesMensuelles(charges);
        dossier.setAncienneteActiviteMois(anciennete);
        dossier.setNbPersonnesCharge(personnesCharge);
    }

    private void verifierAgentProprietaire(
            DossierKyc dossier, UtilisateurAuthentifie agent, String action) {
        boolean memeAgence = agent.agenceId() != null
                && dossier.getAgence().getId().equals(agent.agenceId());
        boolean proprietaire = dossier.getCreePar().getId().equals(agent.id());
        if (!memeAgence || !proprietaire) {
            auditService.accesRefuse(action, "DOSSIER_KYC", dossier.getId());
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCES_REFUSE, "Accès refusé.");
        }
    }
}
