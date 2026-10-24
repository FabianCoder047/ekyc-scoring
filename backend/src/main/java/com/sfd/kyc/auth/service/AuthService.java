package com.sfd.kyc.auth.service;

import com.sfd.kyc.auth.domain.RefreshToken;
import com.sfd.kyc.auth.domain.TentativeConnexion;
import com.sfd.kyc.auth.domain.Utilisateur;
import com.sfd.kyc.auth.repo.RefreshTokenRepository;
import com.sfd.kyc.auth.repo.TentativeConnexionRepository;
import com.sfd.kyc.auth.repo.UtilisateurRepository;
import com.sfd.kyc.auth.web.AuthResponse;
import com.sfd.kyc.auth.web.UtilisateurDto;
import com.sfd.kyc.common.audit.AuditAction;
import com.sfd.kyc.common.audit.AuditService;
import com.sfd.kyc.common.config.AppProperties;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import com.sfd.kyc.common.security.CurrentUser;
import com.sfd.kyc.common.security.JwtService;
import com.sfd.kyc.common.util.Hachage;
import com.sfd.kyc.common.web.RequestContext;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /**
     * Hash BCrypt factice : permet de conserver un temps de réponse comparable
     * lorsque l'email n'existe pas (limite l'énumération de comptes).
     */
    private static final String HASH_FACTICE =
            "$2b$12$N7NTPPTUFo9MA0kHFfZBuuF5P4j2yIUlse5T2U8dna40qL3jx/TaG";

    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TentativeConnexionRepository tentativeConnexionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final AppProperties proprietes;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UtilisateurRepository utilisateurRepository,
            RefreshTokenRepository refreshTokenRepository,
            TentativeConnexionRepository tentativeConnexionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            AppProperties proprietes,
            Clock clock) {
        this.utilisateurRepository = utilisateurRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tentativeConnexionRepository = tentativeConnexionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.proprietes = proprietes;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse connecter(String emailBrut, String motDePasse) {
        String email = normaliser(emailBrut);
        Instant maintenant = clock.instant();

        if (nombreEchecs(email, maintenant) >= proprietes.security().maxTentatives()) {
            enregistrerTentative(email, false, maintenant);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("motif", "verrouillage_temporaire");
            auditService.journaliserPourMemeTransaction(
                    AuditAction.LOGIN_ECHEC, "AUTH", null, null, details, null, null);
            throw tropDeTentatives();
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email).orElse(null);
        String hash = utilisateur == null ? HASH_FACTICE : utilisateur.getMotDePasseHash();
        boolean motDePasseValide = passwordEncoder.matches(motDePasse, hash);
        boolean compteUtilisable = utilisateur != null && utilisateur.isActif();

        if (!motDePasseValide || !compteUtilisable) {
            enregistrerTentative(email, false, maintenant);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("motif", "identifiants_invalides");
            auditService.journaliserPourMemeTransaction(
                    AuditAction.LOGIN_ECHEC,
                    "UTILISATEUR",
                    utilisateur == null ? null : utilisateur.getId(),
                    null,
                    details,
                    utilisateur == null ? null : utilisateur.getId(),
                    utilisateur == null ? null : utilisateur.getRole());
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.AUTHENTIFICATION_ECHOUEE,
                    "Email ou mot de passe invalide.");
        }

        enregistrerTentative(email, true, maintenant);
        AuthResponse reponse = creerSession(utilisateur, maintenant);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", utilisateur.getRole().name());
        auditService.journaliserPourMemeTransaction(
                AuditAction.LOGIN_SUCCES,
                "UTILISATEUR",
                utilisateur.getId(),
                null,
                details,
                utilisateur.getId(),
                utilisateur.getRole());
        return reponse;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse rafraichir(String refreshBrut) {
        Instant maintenant = clock.instant();
        RefreshToken existant = refreshTokenRepository
                .findByTokenHash(Hachage.sha256Hex(refreshBrut))
                .orElseThrow(this::refreshInvalide);

        if (existant.getRevoqueLe() != null) {
            // Réutilisation d'un jeton déjà tourné : on révoque toute la famille.
            refreshTokenRepository.revoquerTousActifs(existant.getUtilisateur().getId(), maintenant);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("motif", "reutilisation_refresh_token");
            auditService.journaliserPourMemeTransaction(
                    AuditAction.TOKEN_REUTILISE,
                    "UTILISATEUR",
                    existant.getUtilisateur().getId(),
                    null,
                    details,
                    existant.getUtilisateur().getId(),
                    existant.getUtilisateur().getRole());
            throw refreshInvalide();
        }

        if (existant.getExpireLe().isBefore(maintenant) || !existant.getUtilisateur().isActif()) {
            throw refreshInvalide();
        }

        Utilisateur utilisateur = existant.getUtilisateur();
        existant.setRevoqueLe(maintenant);
        RefreshToken nouveau = creerRefreshToken(utilisateur, maintenant);
        existant.setRemplacePar(nouveau.getId());

        return new AuthResponse(
                jwtService.genererAccessToken(utilisateur),
                nouveau.getTokenClair(),
                jwtService.accessTtlSecondes(),
                UtilisateurDto.de(utilisateur));
    }

    @Transactional
    public void deconnecter(String refreshBrut) {
        if (refreshBrut == null || refreshBrut.isBlank()) {
            return;
        }
        Long utilisateurId = CurrentUser.get().id();
        refreshTokenRepository.findByTokenHash(Hachage.sha256Hex(refreshBrut))
                .filter(token -> token.getUtilisateur().getId().equals(utilisateurId))
                .filter(token -> token.getRevoqueLe() == null)
                .ifPresent(token -> token.setRevoqueLe(clock.instant()));
    }

    private AuthResponse creerSession(Utilisateur utilisateur, Instant maintenant) {
        RefreshToken refresh = creerRefreshToken(utilisateur, maintenant);
        return new AuthResponse(
                jwtService.genererAccessToken(utilisateur),
                refresh.getTokenClair(),
                jwtService.accessTtlSecondes(),
                UtilisateurDto.de(utilisateur));
    }

    private RefreshToken creerRefreshToken(Utilisateur utilisateur, Instant maintenant) {
        byte[] octets = new byte[32];
        random.nextBytes(octets);
        String clair = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);

        RefreshToken token = new RefreshToken();
        token.setUtilisateur(utilisateur);
        token.setTokenHash(Hachage.sha256Hex(clair));
        token.setExpireLe(maintenant.plus(proprietes.jwt().refreshTtl()));
        token.setCreeLe(maintenant);
        // Champ transitoire, jamais persisté : sert uniquement à renvoyer le jeton au client.
        token.setTokenClair(clair);
        return refreshTokenRepository.saveAndFlush(token);
    }

    private long nombreEchecs(String email, Instant maintenant) {
        return tentativeConnexionRepository.compterEchecsDepuis(
                email, maintenant.minus(proprietes.security().fenetreTentatives()));
    }

    private void enregistrerTentative(String email, boolean succes, Instant maintenant) {
        TentativeConnexion tentative = new TentativeConnexion();
        tentative.setEmail(email);
        tentative.setAdresseIp(RequestContext.adresseIp());
        tentative.setSucces(succes);
        tentative.setHorodatage(maintenant);
        tentativeConnexionRepository.save(tentative);
    }

    private ApiException tropDeTentatives() {
        return new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                ErrorCodes.TROP_DE_TENTATIVES,
                "Trop de tentatives de connexion. Réessayez dans quelques minutes.");
    }

    private ApiException refreshInvalide() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                ErrorCodes.REFRESH_TOKEN_INVALIDE,
                "Session expirée, veuillez vous reconnecter.");
    }

    private String normaliser(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
