package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByNumeroFacture(String numeroFacture);
    boolean existsByNumeroFacture(String numeroFacture);

    List<Facture> findByProjetId(Long projetId);
    List<Facture> findByStatut(Facture.StatutFacture statut);

    // CORRECTION : méthodes manquantes utilisées dans EmployerService.getStats()
    long countByEmetteurId(Long emetteurId);
    long countByClientId(Long clientId);

    @Query("SELECT f FROM Facture f " +
            "WHERE f.dateEcheance < :date " +
            "AND f.statut != :statutPayee " +
            "AND f.statut != :statutAnnulee")
    List<Facture> findFacturesEnRetard(
            @Param("date") LocalDate date,
            @Param("statutPayee") Facture.StatutFacture statutPayee,
            @Param("statutAnnulee") Facture.StatutFacture statutAnnulee
    );

    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f WHERE f.projet.id = :projetId")
    BigDecimal sumMontantTTCByProjetId(@Param("projetId") Long projetId);

    @Query("SELECT COALESCE(SUM(f.montantPaye), 0) FROM Facture f WHERE f.projet.id = :projetId")
    BigDecimal sumMontantPayeByProjetId(@Param("projetId") Long projetId);

    @Query("SELECT COALESCE(SUM(f.montantRestant), 0) FROM Facture f WHERE f.projet.id = :projetId")
    BigDecimal sumMontantRestantByProjetId(@Param("projetId") Long projetId);

    // CORRECTION : méthodes manquantes pour les stats employé
    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f WHERE f.emetteur.id = :emetteurId")
    BigDecimal sumMontantTTCByEmetteurId(@Param("emetteurId") Long emetteurId);

    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f WHERE f.client.id = :clientId")
    BigDecimal sumMontantTTCByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f")
    BigDecimal sumMontantTTCGlobal();

    @Query("SELECT COALESCE(SUM(f.montantPaye), 0) FROM Facture f")
    BigDecimal sumMontantPayeGlobal();

    @Query("SELECT f.statut, COUNT(f) FROM Facture f GROUP BY f.statut")
    List<Object[]> countFacturesByStatut();

    long countByProjetId(Long projetId);
    long countByStatut(Facture.StatutFacture statut);

    @Query("SELECT f FROM Facture f " +
            "LEFT JOIN FETCH f.projet " +
            "LEFT JOIN FETCH f.emetteur " +
            "LEFT JOIN FETCH f.client " +
            "LEFT JOIN FETCH f.lignesFacture " +
            "WHERE f.id = :id")
    Optional<Facture> findByIdWithRelations(@Param("id") Long id);

    // Pour les factures par émetteur/client (listes)
    List<Facture> findByEmetteurId(Long emetteurId);
    List<Facture> findByClientId(Long clientId);
}