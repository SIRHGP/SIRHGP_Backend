package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.EcritureComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EcritureComptableRepository extends JpaRepository<EcritureComptable, Long> {
    List<EcritureComptable> findByFactureIdOrderByDateAscIdAsc(Long factureId);
    List<EcritureComptable> findByJournalOrderByDateAsc(EcritureComptable.Journal journal);
    List<EcritureComptable> findByCompteOrderByDateAsc(String compte);
}
