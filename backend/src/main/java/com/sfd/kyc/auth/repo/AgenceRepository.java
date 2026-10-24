package com.sfd.kyc.auth.repo;

import com.sfd.kyc.auth.domain.Agence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgenceRepository extends JpaRepository<Agence, Long> {
}
