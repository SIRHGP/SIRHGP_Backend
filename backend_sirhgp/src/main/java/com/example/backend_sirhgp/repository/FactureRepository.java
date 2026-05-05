package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // ─── Recherches par relations ─────────────────────────────────────────────
    List<Facture> findByProjetId(Long projetId);
    List<Facture> findByStatut(Facture.StatutFacture statut);
    List<Facture> findByEmetteurId(Long emetteurId);
    List<Facture> findByClientId(Long clientId);

    // ─── Dispatch ────────────────────────────────────────────────────────────
    List<Facture> findByClientIdAndEstConsulteeFalse(Long clientId);
    List<Facture> findByClientIdAndEstConsulteeTrue(Long clientId);

    @Modifying
    @Query("UPDATE Facture f SET f.estConsultee = true, f.dateConsultation = CURRENT_TIMESTAMP " +
            "WHERE f.id = :factureId AND f.client.id = :clientId")
    void markAsConsulted(@Param("factureId") Long factureId,
                         @Param("clientId") Long clientId);

    // ─── Stats ───────────────────────────────────────────────────────────────
    long countByEmetteurId(Long emetteurId);
    long countByClientId(Long clientId);
    long countByProjetId(Long projetId);

    // ─── Factures en retard ─────────────────────────────────────────────────
    @Query("SELECT f FROM Facture f " +
            "WHERE f.dateEcheance < :date " +
            "AND f.statut != :statutPayee " +
            "AND f.statut != :statutAnnulee")
    List<Facture> findFacturesEnRetard(
            @Param("date") LocalDate date,
            @Param("statutPayee") Facture.StatutFacture statutPayee,
            @Param("statutAnnulee") Facture.StatutFacture statutAnnulee);

    // ─── Sommes pour projets ────────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f WHERE f.projet.id = :projetId")
    BigDecimal sumMontantTTCByProjetId(@Param("projetId") Long projetId);

    @Query("SELECT COALESCE(SUM(f.montantPaye), 0) FROM Facture f WHERE f.projet.id = :projetId")
    BigDecimal sumMontantPayeByProjetId(@Param("projetId") Long projetId);

    @Query("SELECT COALESCE(SUM(f.montantRestant), 0) FROM Facture f WHERE f.projet.id = :projetId")
    BigDecimal sumMontantRestantByProjetId(@Param("projetId") Long projetId);

    // ─── Sommes pour employés ────────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f WHERE f.emetteur.id = :emetteurId")
    BigDecimal sumMontantTTCByEmetteurId(@Param("emetteurId") Long emetteurId);

    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f WHERE f.client.id = :clientId")
    BigDecimal sumMontantTTCByClientId(@Param("clientId") Long clientId);

    // ─── Sommes globales ────────────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f")
    BigDecimal sumMontantTTCGlobal();

    @Query("SELECT COALESCE(SUM(f.montantPaye), 0) FROM Facture f")
    BigDecimal sumMontantPayeGlobal();

    // ─── Stats par statut ───────────────────────────────────────────────────
    @Query("SELECT f.statut, COUNT(f) FROM Facture f GROUP BY f.statut")
    List<Object[]> countFacturesByStatut();

    long countByStatut(Facture.StatutFacture statut);

    // ─── Chargement avec relations (évite N+1) ──────────────────────────────
    @Query("SELECT f FROM Facture f " +
            "LEFT JOIN FETCH f.projet " +
            "LEFT JOIN FETCH f.emetteur " +
            "LEFT JOIN FETCH f.client " +
            "LEFT JOIN FETCH f.lignesFacture " +
            "WHERE f.id = :id")
    Optional<Facture> findByIdWithRelations(@Param("id") Long id);
}