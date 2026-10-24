package com.sfd.kyc.admin.web;

import com.sfd.kyc.auth.domain.Agence;
import com.sfd.kyc.auth.domain.Role;
import com.sfd.kyc.auth.domain.Utilisateur;
import com.sfd.kyc.auth.repo.AgenceRepository;
import com.sfd.kyc.auth.repo.UtilisateurRepository;
import com.sfd.kyc.common.audit.AuditAction;
import com.sfd.kyc.common.audit.AuditService;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import com.sfd.kyc.common.security.CurrentUser;
import com.sfd.kyc.kyc.domain.CritereScoring;
import com.sfd.kyc.kyc.domain.RegleScoring;
import com.sfd.kyc.kyc.repo.RegleScoringRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final AgenceRepository agenceRepository;
    private final RegleScoringRepository regleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public AdminService(
            UtilisateurRepository utilisateurRepository,
            AgenceRepository agenceRepository,
            RegleScoringRepository regleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            Clock clock) {
        this.utilisateurRepository = utilisateurRepository;
        this.agenceRepository = agenceRepository;
        this.regleRepository = regleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurAdminDto> listerUtilisateurs() {        return utilisateurRepository.findAll(Sort.by("nom", "prenom")).stream()
                .map(UtilisateurAdminDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgenceDto> listerAgences() {
        return agenceRepository.findAll(Sort.by("nom")).stream()
                .map(AgenceDto::de)
                .toList();
    }

    @Transactional
    public UtilisateurAdminDto creerUtilisateur(UtilisateurCreationRequest demande) {
        String email = demande.email().trim().toLowerCase(Locale.ROOT);
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, ErrorCodes.EMAIL_DEJA_UTILISE, "Cet email est déjà utilisé.");
        }
        Agence agence = resoudreAgence(demande.agenceId(), demande.role());

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setMotDePasseHash(passwordEncoder.encode(demande.motDePasse()));
        utilisateur.setNom(demande.nom().trim());
        utilisateur.setPrenom(demande.prenom().trim());
        utilisateur.setRole(demande.role());
        utilisateur.setAgence(agence);
        utilisateur.setActif(true);
        utilisateur.setCreeLe(clock.instant());
        utilisateur = utilisateurRepository.saveAndFlush(utilisateur);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", utilisateur.getRole().name());
        details.put("actif", true);
        auditService.journaliser(
                AuditAction.UTILISATEUR_CREE, "UTILISATEUR", utilisateur.getId(), null, details);
        return UtilisateurAdminDto.de(utilisateur);
    }

    @Transactional
    public UtilisateurAdminDto modifierUtilisateur(Long id, UtilisateurModificationRequest demande) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
        Long courant = CurrentUser.get().id();
        if (utilisateur.getId().equals(courant) && !demande.actif()) {
            throw ApiException.validation("Vous ne pouvez pas désactiver votre propre compte.");
        }
        Agence agence = resoudreAgence(demande.agenceId(), demande.role());

        utilisateur.setNom(demande.nom().trim());
        utilisateur.setPrenom(demande.prenom().trim());
        utilisateur.setRole(demande.role());
        utilisateur.setAgence(agence);
        utilisateur.setActif(demande.actif());
        if (demande.nouveauMotDePasse() != null && !demande.nouveauMotDePasse().isBlank()) {
            utilisateur.setMotDePasseHash(passwordEncoder.encode(demande.nouveauMotDePasse()));
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", utilisateur.getRole().name());
        details.put("actif", utilisateur.isActif());
        auditService.journaliser(
                AuditAction.UTILISATEUR_MODIFIE, "UTILISATEUR", utilisateur.getId(), null, details);
        return UtilisateurAdminDto.de(utilisateur);
    }

    /** Suppression logique : les références d'audit et les dossiers restent intacts. */
    @Transactional
    public void desactiverUtilisateur(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
        if (utilisateur.getId().equals(CurrentUser.get().id())) {
            throw ApiException.validation("Vous ne pouvez pas désactiver votre propre compte.");
        }
        utilisateur.setActif(false);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("actif", false);
        auditService.journaliser(
                AuditAction.UTILISATEUR_MODIFIE, "UTILISATEUR", utilisateur.getId(), null, details);
    }

    @Transactional(readOnly = true)
    public List<CritereDto> listerCriteres() {
        return regleRepository.findAll(Sort.by("critere", "borneMin")).stream()
                .map(CritereDto::de)
                .toList();
    }

    @Transactional
    public CritereDto creerCritere(CritereRequest demande) {
        verifierBornes(demande);
        verifierChevauchement(demande, null);
        RegleScoring regle = new RegleScoring();
        appliquer(regle, demande);
        regle = regleRepository.saveAndFlush(regle);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("critere", regle.getCritere().name());
        details.put("points", regle.getPoints());
        details.put("actif", regle.isActif());
        auditService.journaliser(
                AuditAction.CRITERE_CREE, "CRITERE_SCORING", regle.getId(), null, details);
        return CritereDto.de(regle);
    }

    @Transactional
    public CritereDto modifierCritere(Long id, CritereRequest demande) {
        RegleScoring regle = regleRepository.findById(id)
                .orElseThrow(() -> ApiException.introuvable("Critère de scoring introuvable."));
        verifierBornes(demande);
        verifierChevauchement(demande, id);
        appliquer(regle, demande);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("critere", regle.getCritere().name());
        details.put("points", regle.getPoints());
        details.put("actif", regle.isActif());
        auditService.journaliser(
                AuditAction.CRITERE_MODIFIE, "CRITERE_SCORING", regle.getId(), null, details);
        return CritereDto.de(regle);
    }

    @Transactional
    public void supprimerCritere(Long id) {
        RegleScoring regle = regleRepository.findById(id)
                .orElseThrow(() -> ApiException.introuvable("Critère de scoring introuvable."));
        Map<String, Object> avant = new LinkedHashMap<>();
        avant.put("critere", regle.getCritere().name());
        avant.put("points", regle.getPoints());
        avant.put("actif", regle.isActif());
        auditService.journaliser(
                AuditAction.CRITERE_SUPPRIME, "CRITERE_SCORING", regle.getId(), avant, null);
        regleRepository.delete(regle);
    }

    private void appliquer(RegleScoring regle, CritereRequest demande) {
        regle.setCritere(demande.critere());
        regle.setBorneMin(demande.borneMin());
        regle.setBorneMax(demande.borneMax());
        regle.setPoints(demande.points());
        regle.setLibelle(demande.libelle().trim());
        regle.setActif(demande.actif());
    }

    private void verifierBornes(CritereRequest demande) {
        if (demande.borneMin() != null && demande.borneMax() != null
                && demande.borneMin().compareTo(demande.borneMax()) > 0) {
            throw ApiException.validation("La borne minimale doit être inférieure ou égale à la borne maximale.");
        }
    }

    private void verifierChevauchement(CritereRequest demande, Long idExclu) {
        if (!demande.actif()) {
            return;
        }
        for (RegleScoring existante : regleRepository.findByCritereOrderByBorneMinAsc(demande.critere())) {
            if (!existante.isActif() || (idExclu != null && existante.getId().equals(idExclu))) {
                continue;
            }
            if (chevauche(demande.borneMin(), demande.borneMax(),
                    existante.getBorneMin(), existante.getBorneMax())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCodes.CHEVAUCHEMENT_CRITERES,
                        "Cette tranche chevauche une tranche active existante.");
            }
        }
    }

    private boolean chevauche(BigDecimal minA, BigDecimal maxA, BigDecimal minB, BigDecimal maxB) {
        boolean aCommenceAvantBFin = maxB == null || minA == null || minA.compareTo(maxB) <= 0;
        boolean bCommenceAvantAFin = maxA == null || minB == null || minB.compareTo(maxA) <= 0;
        return aCommenceAvantBFin && bCommenceAvantAFin;
    }

    private Agence resoudreAgence(Long agenceId, Role role) {
        if (agenceId == null) {
            if (role == Role.AGENT_ACCUEIL) {
                throw ApiException.validation("Une agence est obligatoire pour un agent d'accueil.");
            }
            return null;
        }
        return agenceRepository.findById(agenceId)
                .orElseThrow(() -> ApiException.introuvable("Agence introuvable."));
    }
}
