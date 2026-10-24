package com.sfd.kyc.kyc.repo;

import com.sfd.kyc.kyc.domain.Client;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByIdAndAgenceId(Long id, Long agenceId);

    List<Client> findByAgenceIdOrderByNomAscPrenomAsc(Long agenceId);
}
