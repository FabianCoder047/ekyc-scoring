package com.sfd.kyc.kyc.web;

import com.sfd.kyc.common.web.PageResponse;
import com.sfd.kyc.kyc.domain.StatutDossier;
import com.sfd.kyc.kyc.domain.TypeDocument;
import com.sfd.kyc.kyc.service.DocumentService;
import com.sfd.kyc.kyc.service.DocumentTelecharge;
import com.sfd.kyc.kyc.service.DossierService;
import com.sfd.kyc.kyc.service.ScoringService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/kyc/dossiers")
public class DossierController {

    private final DossierService dossierService;
    private final DocumentService documentService;
    private final ScoringService scoringService;

    public DossierController(
            DossierService dossierService,
            DocumentService documentService,
            ScoringService scoringService) {
        this.dossierService = dossierService;
        this.documentService = documentService;
        this.scoringService = scoringService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DossierDetail creer(@Valid @RequestBody DossierCreationRequest demande) {
        return dossierService.creer(demande);
    }

    @PutMapping("/{id}")
    public DossierDetail modifier(
            @PathVariable Long id, @Valid @RequestBody DossierModificationRequest demande) {
        return dossierService.modifier(id, demande);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto deposerDocument(
            @PathVariable Long id,
            @RequestParam("type") TypeDocument type,
            @RequestParam("fichier") MultipartFile fichier) {
        return documentService.deposer(id, type, fichier);
    }

    @GetMapping("/{id}/documents/{documentId}/contenu")
    public ResponseEntity<byte[]> contenu(
            @PathVariable Long id, @PathVariable Long documentId) {
        DocumentTelecharge document = documentService.telecharger(id, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document\"")
                .contentLength(document.contenu().length)
                .body(document.contenu());
    }

    @PostMapping("/{id}/soumission")
    public DossierDetail soumettre(@PathVariable Long id) {
        return dossierService.soumettre(id);
    }

    @PostMapping("/{id}/prise-en-charge")
    public DossierDetail prendreEnCharge(@PathVariable Long id) {
        return dossierService.prendreEnCharge(id);
    }

    @PostMapping("/{id}/scoring")
    public ResultatScoringDto scorer(@PathVariable Long id) {
        return scoringService.calculer(id);
    }

    @PostMapping("/{id}/decision")
    public DossierDetail decider(@PathVariable Long id, @Valid @RequestBody DecisionRequest demande) {
        return dossierService.decider(id, demande);
    }

    @GetMapping
    public PageResponse<DossierResume> lister(
            @RequestParam(required = false) StatutDossier statut,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        return dossierService.lister(statut, q, page, taille);
    }

    @GetMapping("/{id}")
    public DossierDetail consulter(@PathVariable Long id) {
        return dossierService.consulter(id);
    }

    @GetMapping("/{id}/historique")
    public HistoriqueDto historique(@PathVariable Long id) {
        return dossierService.historique(id);
    }
}
