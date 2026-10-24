package com.sfd.kyc.common.security;

import com.sfd.kyc.auth.domain.Utilisateur;
import com.sfd.kyc.auth.repo.UtilisateurRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UtilisateurRepository utilisateurRepository) {
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String entete = request.getHeader("Authorization");
        if (entete == null || !entete.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = entete.substring(7).trim();
        try {
            Claims claims = jwtService.valider(token);
            Long utilisateurId = claims.get("uid", Long.class);
            if (utilisateurId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Utilisateur utilisateur = utilisateurRepository.findAvecAgenceById(utilisateurId).orElse(null);
                if (utilisateur != null && utilisateur.isActif()) {
                    UtilisateurAuthentifie principal = new UtilisateurAuthentifie(
                            utilisateur.getId(),
                            utilisateur.getEmail(),
                            utilisateur.getRole(),
                            utilisateur.getAgence() == null ? null : utilisateur.getAgence().getId());
                    var authentification = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(utilisateur.getRole().authority())));
                    authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentification);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
