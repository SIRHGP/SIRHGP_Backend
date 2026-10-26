package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    Page<AuditLog> findByUserIdIn(List<Long> userIds, Pageable pageable);
    Page<AuditLog> findByEntityTypeAndEntityIdAndUserIdIn(String entityType, Long entityId,
                                                           List<Long> userIds, Pageable pageable);

    Page<AuditLog> findByActionType(AuditActionType actionType, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    List<AuditLog> findByUserIdAndTimestampAfter(Long userId, LocalDateTime timestamp);

    /**
     * Recherche avancée avec filtres multiples (tous optionnels).
     * Le filtre success a été retiré — toutes les actions sont listées.
     * Le paramètre search permet de filtrer par nom/email utilisateur, description ou type d'entité.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:actionType IS NULL OR a.actionType = :actionType) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
            "(:toDate IS NULL OR a.timestamp <= :toDate) AND " +
            "(:entrepriseId IS NULL OR a.userId IN " +
            " (SELECT e.id FROM Employer e WHERE e.entreprise.id = :entrepriseId)) AND " +
            // 5.9 (P1-10) : scoping fin MANAGER — auteurs rattachés à une succursale.
            // Paramètre scalaire nullable : même motif null-safe que `entrepriseId`.
            "(:sucursalId IS NULL OR a.userId IN " +
            " (SELECT e2.id FROM Employer e2 WHERE e2.sucursal.id = :sucursalId)) AND " +
            "(:search IS NULL OR " +
            "LOWER(a.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.actionDescription) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.entityType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> search(@Param("userId") Long userId,
                          @Param("actionType") AuditActionType actionType,
                          @Param("entityType") String entityType,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          @Param("search") String search,
                          @Param("entrepriseId") Long entrepriseId,
                          @Param("sucursalId") Long sucursalId,
                          Pageable pageable);

    /**
     * 5.9 (P1-10) : même recherche avancée, restreinte aux auteurs rattachés à l'un
     * des départements du périmètre (LEAD). Requête dédiée plutôt que paramètre
     * optionnel : une liste vide dans un `IN :param` n'est pas portable en JPQL, et
     * la liste transmise est toujours non vide (le service retombe sur
     * {@link #search} sinon).
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:actionType IS NULL OR a.actionType = :actionType) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
            "(:toDate IS NULL OR a.timestamp <= :toDate) AND " +
            "(:entrepriseId IS NULL OR a.userId IN " +
            " (SELECT e.id FROM Employer e WHERE e.entreprise.id = :entrepriseId)) AND " +
            "a.userId IN (SELECT e3.id FROM Employer e3 " +
            " WHERE e3.poste.departement.id IN :departementIds) AND " +
            "(:search IS NULL OR " +
            "LOWER(a.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.actionDescription) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.entityType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchByDepartementIds(@Param("userId") Long userId,
                                          @Param("actionType") AuditActionType actionType,
                                          @Param("entityType") String entityType,
                                          @Param("fromDate") LocalDateTime fromDate,
                                          @Param("toDate") LocalDateTime toDate,
                                          @Param("search") String search,
                                          @Param("entrepriseId") Long entrepriseId,
                                          @Param("departementIds") List<Long> departementIds,
                                          Pageable pageable);

    // ─── Refonte 5.9 : scoping par entreprise (ADMIN / MANAGER / LEAD) ────────

    /** Journaux dont l'auteur appartient à une entreprise donnée. */
    @Query("SELECT a FROM AuditLog a WHERE a.userId IN " +
            " (SELECT e.id FROM Employer e WHERE e.entreprise.id = :entrepriseId) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserEntrepriseId(@Param("entrepriseId") Long entrepriseId, Pageable pageable);

    /**
     * 5.9 (P1-10) : journaux dont l'auteur est rattaché à une succursale donnée
     * (périmètre MANAGER). Le rattachement de référence est `Employer.sucursal`,
     * identique au scoping déjà appliqué par `EmployerService` / `PaieService` /
     * `SucursalService` — un auteur sans succursale n'apparaît donc pas ici.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId IN " +
            " (SELECT e.id FROM Employer e WHERE e.sucursal.id = :sucursalId) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserSucursalId(@Param("sucursalId") Long sucursalId, Pageable pageable);

    /**
     * 5.9 (P1-10) : journaux dont l'auteur exerce dans l'un des départements du
     * périmètre (LEAD = départements supervisés + département de son poste).
     * La liste est toujours non vide (cf. {@link #searchByDepartementIds}).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId IN " +
            " (SELECT e.id FROM Employer e WHERE e.poste.departement.id IN :departementIds) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserDepartementIds(@Param("departementIds") List<Long> departementIds,
                                            Pageable pageable);

    /** Anomalies : échecs de connexion groupés par email (seuil paramétrable). */
    @Query("SELECT a.userEmail, COUNT(a) FROM AuditLog a " +
            "WHERE a.actionType = :actionType AND a.timestamp >= :from " +
            "GROUP BY a.userEmail HAVING COUNT(a) >= :min " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> loginFailuresSince(@Param("actionType") AuditActionType actionType,
                                      @Param("from") LocalDateTime from,
                                      @Param("min") long min);

    /** Variante scopée : seuls les emails des comptes de l'entreprise. */
    @Query("SELECT a.userEmail, COUNT(a) FROM AuditLog a " +
            "WHERE a.actionType = :actionType AND a.timestamp >= :from " +
            "AND a.userEmail IN (SELECT e.email FROM Employer e WHERE e.entreprise.id = :entrepriseId) " +
            "GROUP BY a.userEmail HAVING COUNT(a) >= :min " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> loginFailuresByEntreprise(@Param("actionType") AuditActionType actionType,
                                             @Param("from") LocalDateTime from,
                                             @Param("entrepriseId") Long entrepriseId,
                                             @Param("min") long min);

    /** Nombre d'actions d'un type depuis une date (spikes de suppression). */
    long countByActionTypeAndTimestampAfter(AuditActionType actionType, LocalDateTime from);

    /** Variante scopée par entreprise de l'auteur. */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType = :actionType AND a.timestamp >= :from " +
            "AND a.userId IN (SELECT e.id FROM Employer e WHERE e.entreprise.id = :entrepriseId)")
    long countByActionTypeSinceForEntreprise(@Param("actionType") AuditActionType actionType,
                                             @Param("from") LocalDateTime from,
                                             @Param("entrepriseId") Long entrepriseId);

    /**
     * Statistiques : nombre d'actions par type sur une période.
     */
    @Query("SELECT a.actionType, COUNT(a) FROM AuditLog a " +
            "WHERE a.timestamp >= :fromDate AND a.timestamp <= :toDate " +
            "GROUP BY a.actionType ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionTypeBetween(@Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDate") LocalDateTime toDate);

    /**
     * Top utilisateurs les plus actifs sur une période.
     */
    @Query("SELECT a.userEmail, a.userName, COUNT(a) FROM AuditLog a " +
            "WHERE a.timestamp >= :fromDate AND a.timestamp <= :toDate AND a.userEmail IS NOT NULL " +
            "GROUP BY a.userEmail, a.userName ORDER BY COUNT(a) DESC")
    List<Object[]> topActiveUsers(@Param("fromDate") LocalDateTime fromDate,
                                  @Param("toDate") LocalDateTime toDate,
                                  Pageable pageable);

    long countByTimestampBetween(LocalDateTime fromDate, LocalDateTime toDate);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId IN :userIds " +
            "AND a.timestamp >= :fromDate AND a.timestamp <= :toDate")
    long countByUserIdsAndTimestampBetween(@Param("userIds") List<Long> userIds,
                                           @Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate);

    @Query("SELECT a.actionType, COUNT(a) FROM AuditLog a WHERE a.userId IN :userIds " +
            "AND a.timestamp >= :fromDate AND a.timestamp <= :toDate " +
            "GROUP BY a.actionType ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionTypeBetweenForUsers(@Param("userIds") List<Long> userIds,
                                                     @Param("fromDate") LocalDateTime fromDate,
                                                     @Param("toDate") LocalDateTime toDate);

    @Query("SELECT a.userEmail, a.userName, COUNT(a) FROM AuditLog a WHERE a.userId IN :userIds " +
            "AND a.timestamp >= :fromDate AND a.timestamp <= :toDate AND a.userEmail IS NOT NULL " +
            "GROUP BY a.userEmail, a.userName ORDER BY COUNT(a) DESC")
    List<Object[]> topActiveUsersForUsers(@Param("userIds") List<Long> userIds,
                                          @Param("fromDate") LocalDateTime fromDate,
                                          @Param("toDate") LocalDateTime toDate,
                                          Pageable pageable);
}
