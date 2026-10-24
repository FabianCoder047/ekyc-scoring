package com.sfd.kyc.kyc.service;

import com.sfd.kyc.auth.domain.Role;
import com.sfd.kyc.auth.repo.UtilisateurRepository;
import com.sfd.kyc.common.audit.AuditAction;
import com.sfd.kyc.common.audit.AuditService;
import com.sfd.kyc.common.config.AppProperties;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import com.sfd.kyc.common.security.CurrentUser;
import com.sfd.kyc.common.security.UtilisateurAuthentifie;
import com.sfd.kyc.common.util.Hachage;
import com.sfd.kyc.kyc.domain.DocumentKyc;
import com.sfd.kyc.kyc.domain.DossierKyc;
import com.sfd.kyc.kyc.domain.StatutDossier;
import com.sfd.kyc.kyc.domain.TypeDocument;
import com.sfd.kyc.kyc.repo.DocumentKycRepository;
import com.sfd.kyc.kyc.repo.DossierKycRepository;
import com.sfd.kyc.kyc.web.DocumentDto;
import java.io.IOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final Set<String> TYPES_ACCEPTES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    private final DossierKycRepository dossierRepository;
    private final DocumentKycRepository documentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FileStorage fileStorage;
    private final ChiffrementService chiffrementService;
    private final AuditService auditService;
    private final AppProperties proprietes;
    private final Clock clock;
    private final Tika tika = new Tika();

    public DocumentService(
            DossierKycRepository dossierRepository,
            DocumentKycRepository documentRepository,
            UtilisateurRepository utilisateurRepository,
            FileStorage fileStorage,
            ChiffrementService chiffrementService,
            AuditService auditService,
            AppProperties proprietes,
            Clock clock) {
        this.dossierRepository = dossierRepository;
        this.documentRepository = documentRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.fileStorage = fileStorage;
        this.chiffrementService = chiffrementService;
        this.auditService = auditService;
        this.proprietes = proprietes;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('AGENT_ACCUEIL')")
    @Transactional
    public DocumentDto deposer(Long dossierId, TypeDocument type, MultipartFile fichier) {
        if (type == null) {
            throw ApiException.validation("Le type de document est obligatoire.");
        }
        UtilisateurAuthentifie agent = CurrentUser.get();
        DossierKyc dossier = dossierRepository.findPourMiseAJour(dossierId)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));

        verifierAgentProprietaire(dossier, agent);
        if (dossier.getStatut() != StatutDossier.BROUILLON) {
            throw ApiException.transition("Les documents ne peuvent être déposés que sur un dossier brouillon.");
        }

        if (fichier == null || fichier.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.FICHIER_ILLISIBLE, "Le fichier est vide.");
        }
        if (fichier.getSize() > proprietes.documents().tailleMaxOctets()) {
            throw new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    ErrorCodes.FICHIER_TROP_VOLUMINEUX,
                    "Le fichier dépasse la taille maximale autorisée (5 Mo).");
        }

        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, ErrorCodes.FICHIER_ILLISIBLE, "Le fichier n'a pas pu être lu.");
        }

        String typeDetecte = tika.detect(contenu);
        if (typeDetecte == null || !TYPES_ACCEPTES.contains(typeDetecte.toLowerCase(Locale.ROOT))) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    ErrorCodes.TYPE_DOCUMENT_NON_SUPPORTE,
                    "Seuls les fichiers JPEG, PNG et PDF sont acceptés.");
        }

        String cle = UUID.randomUUID().toString();
        ChiffrementService.Chiffre chiffre = chiffrementService.chiffrer(contenu);
        fileStorage.enregistrer(cle, chiffre.contenu());

        DocumentKyc ancien = documentRepository
                .findFirstByDossierIdAndTypeAndRemplaceParIsNullOrderByDeposeLeDesc(dossierId, type)
                .orElse(null);

        DocumentKyc document = new DocumentKyc();
        document.setDossier(dossier);
        document.setType(type);
        document.setNomOriginal(nomSur(fichier.getOriginalFilename()));
        document.setCleStockage(cle);
        document.setMimeType(typeDetecte);
        document.setTailleOctets(contenu.length);
        document.setSha256(Hachage.sha256Hex(contenu));
        document.setIv(chiffre.iv());
        document.setDeposePar(utilisateurRepository.getReferenceById(agent.id()));
        document.setDeposeLe(clock.instant());
        document = documentRepository.saveAndFlush(document);
        if (ancien != null) {
            ancien.setRemplacePar(document.getId());
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("documentId", document.getId());
        details.put("type", type.name());
        details.put("tailleOctets", document.getTailleOctets());
        auditService.journaliser(AuditAction.DOCUMENT_DEPOSE, "DOCUMENT_KYC", document.getId(), null, details);

        return DocumentDto.de(document);
    }

    @PreAuthorize("hasAnyRole('ANALYSTE_KYC','SUPERVISEUR')")
    @Transactional
    public DocumentTelecharge telecharger(Long dossierId, Long documentId) {
        UtilisateurAuthentifie utilisateur = CurrentUser.get();
        DossierKyc dossier = dossierRepository.findDetailById(dossierId)
                .orElseThrow(() -> ApiException.introuvable("Dossier introuvable."));

        if (utilisateur.role() == Role.ANALYSTE_KYC) {
            boolean assigne = dossier.getAssigneA() != null
                    && dossier.getAssigneA().getId().equals(utilisateur.id());
            if (!assigne) {
                auditService.accesRefuse("DOCUMENT_CONSULTE", "DOCUMENT_KYC", documentId);
                throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCES_REFUSE, "Accès refusé.");
            }
        }

        DocumentKyc document = documentRepository.findByIdAndDossierId(documentId, dossierId)
                .orElseThrow(() -> ApiException.introuvable("Document introuvable."));

        byte[] chiffre;
        try {
            chiffre = fileStorage.lire(document.getCleStockage());
        } catch (ApiException e) {
            log.warn("Fichier de document absent du stockage (documentId={})", documentId);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.DOCUMENT_ALTERE,
                    "Le document est corrompu ou a été altéré.");
        }

        byte[] clair = chiffrementService.dechiffrer(chiffre, document.getIv());
        String empreinte = Hachage.sha256Hex(clair);
        if (!Hachage.egalConstant(document.getSha256(), empreinte)) {
            log.warn("Empreinte SHA-256 incohérente (documentId={})", documentId);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.DOCUMENT_ALTERE,
                    "Le document est corrompu ou a été altéré.");
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("documentId", document.getId());
        details.put("dossierId", dossierId);
        auditService.journaliser(AuditAction.DOCUMENT_CONSULTE, "DOCUMENT_KYC", document.getId(), null, details);

        return new DocumentTelecharge(clair, document.getMimeType());
    }

    private void verifierAgentProprietaire(DossierKyc dossier, UtilisateurAuthentifie agent) {
        boolean memeAgence = agent.agenceId() != null
                && dossier.getAgence().getId().equals(agent.agenceId());
        boolean proprietaire = dossier.getCreePar().getId().equals(agent.id());
        if (!memeAgence || !proprietaire) {
            auditService.accesRefuse("DOCUMENT_DEPOSE", "DOSSIER_KYC", dossier.getId());
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCES_REFUSE, "Accès refusé.");
        }
    }

    private String nomSur(String nomBrut) {
        if (nomBrut == null || nomBrut.isBlank()) {
            return "document";
        }
        String nom = nomBrut.replace('\\', '/');
        int index = nom.lastIndexOf('/');
        if (index >= 0) {
            nom = nom.substring(index + 1);
        }
        nom = nom.replaceAll("[\\p{Cntrl}]", "").trim();
        if (nom.isBlank()) {
            return "document";
        }
        return nom.length() > 200 ? nom.substring(0, 200) : nom;
    }
}
