package com.sfd.kyc.common.error;

import com.sfd.kyc.common.audit.AuditService;
import com.sfd.kyc.common.web.RequestContext;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> api(ApiException exception) {
        ProblemDetail probleme = probleme(exception.status(), exception.code(), exception.getMessage());
        exception.proprietes().forEach(probleme::setProperty);
        return reponse(exception.status(), probleme);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception) {
        List<Map<String, String>> erreurs = exception.getBindingResult().getFieldErrors().stream()
                .map(this::erreurChamp)
                .toList();
        ProblemDetail probleme = probleme(
                HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ECHEC, "La requête contient des champs invalides.");
        probleme.setProperty("erreurs", erreurs);
        return reponse(HttpStatus.BAD_REQUEST, probleme);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> validationParametres(HandlerMethodValidationException exception) {
        ProblemDetail probleme = probleme(
                HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ECHEC, "Paramètre de requête invalide.");
        return reponse(HttpStatus.BAD_REQUEST, probleme);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ProblemDetail> requeteIllisible(Exception exception) {
        return api(new ApiException(
                HttpStatus.BAD_REQUEST, ErrorCodes.REQUETE_INVALIDE, "La requête est invalide."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> fichierTropVolumineux(MaxUploadSizeExceededException exception) {
        return api(new ApiException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCodes.FICHIER_TROP_VOLUMINEUX,
                "Le fichier dépasse la taille maximale autorisée (5 Mo)."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> accesRefuse(AccessDeniedException exception) {
        try {
            String route = RequestContext.courante()
                    .map(request -> request.getMethod() + " " + request.getRequestURI())
                    .orElse(null);
            auditService.accesRefuse(route, "ROUTE", null);
        } catch (RuntimeException e) {
            log.warn("Impossible de journaliser un refus d'accès ({})", e.getClass().getSimpleName());
        }
        return api(new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCES_REFUSE, "Accès refusé."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> authentification(AuthenticationException exception) {
        return api(new ApiException(
                HttpStatus.UNAUTHORIZED, ErrorCodes.TOKEN_INVALIDE, "Authentification requise."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflit(DataIntegrityViolationException exception) {
        log.warn("Violation d'intégrité en base");
        return api(new ApiException(
                HttpStatus.CONFLICT, ErrorCodes.CONFLIT, "L'opération entre en conflit avec des données existantes."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> requeteInvalide(IllegalArgumentException exception) {
        return api(new ApiException(
                HttpStatus.BAD_REQUEST, ErrorCodes.REQUETE_INVALIDE, "La requête est invalide."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> interne(Exception exception) {
        // Aucun contenu de requête, aucune donnée personnelle dans les logs.
        log.error("Erreur interne non gérée (type={})", exception.getClass().getSimpleName());
        return api(new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.ERREUR_INTERNE, "Une erreur interne est survenue."));
    }

    private Map<String, String> erreurChamp(FieldError erreur) {
        return Map.of("champ", erreur.getField(), "message", erreur.getDefaultMessage() == null
                ? "Valeur invalide" : erreur.getDefaultMessage());
    }

    private ProblemDetail probleme(HttpStatus statut, String code, String detail) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(statut.getReasonPhrase());
        probleme.setType(URI.create("about:blank"));
        probleme.setProperty("code", code);
        return probleme;
    }

    private ResponseEntity<ProblemDetail> reponse(HttpStatus statut, ProblemDetail probleme) {
        return ResponseEntity.status(statut)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(probleme);
    }
}
