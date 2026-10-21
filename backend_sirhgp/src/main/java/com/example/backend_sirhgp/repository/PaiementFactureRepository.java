package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.PaiementFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementFactureRepository extends JpaRepository<PaiementFacture, Long> {
    List<PaiementFacture> findByFactureIdOrderByDatePaiementDesc(Long factureId);
}
