package com.sfd.kyc.auth.repo;

import com.sfd.kyc.auth.domain.Utilisateur;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    @EntityGraph(attributePaths = "agence")
    Optional<Utilisateur> findAvecAgenceById(Long id);

    @EntityGraph(attributePaths = "agence")
    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
