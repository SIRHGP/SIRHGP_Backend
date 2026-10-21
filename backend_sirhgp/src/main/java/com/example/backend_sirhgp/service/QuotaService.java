package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.Quota;
import com.example.backend_sirhgp.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Refonte 5.9 (§5) — quotas de la plateforme.
 * Logique : quota d'abord par entreprise, sinon quota global par défaut (entreprise NULL),
 * sinon illimité (null). Enforcement appelé par register-entreprise, EmployerService.create,
 * SucursalService.create et DocumentService.uploadWithDispatch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final QuotaRepository quotaRepository;

    /** Quota effectif pour une entreprise (spécifique sinon global sinon null). */
    @Transactional(readOnly = true)
    public Quota effective(Long entrepriseId) {
        if (entrepriseId != null) {
            var specific = quotaRepository.findByEntrepriseId(entrepriseId);
            if (specific.isPresent()) return specific.get();
        }
        return quotaRepository.findByEntrepriseIdIsNull().orElse(null);
    }

    public Integer maxEmployesOf(Long entrepriseId) {
        Quota q = effective(entrepriseId);
        return q != null ? q.getMaxEmployes() : null;
    }

    public Integer maxSucursalesOf(Long entrepriseId) {
        Quota q = effective(entrepriseId);
        return q != null ? q.getMaxSucursales() : null;
    }

    public Long maxStockageMoOf(Long entrepriseId) {
        Quota q = effective(entrepriseId);
        return q != null ? q.getMaxStockageMo() : null;
    }

    // ─── Gestion (SUPER_ADMIN, /api/plateforme/quotas) ────────────────────────

    @Transactional(readOnly = true)
    public List<Quota> findAll() {
        return quotaRepository.findAllByOrderByEntrepriseIdAsc();
    }

    @Transactional(readOnly = true)
    public Quota findByEntrepriseIdOrNull(Long entrepriseId) {
        if (entrepriseId == null) return quotaRepository.findByEntrepriseIdIsNull().orElse(null);
        return quotaRepository.findByEntrepriseId(entrepriseId).orElse(null);
    }

    @Transactional
    public Quota upsert(Long entrepriseId, Integer maxEmployes, Integer maxSucursales, Long maxStockageMo) {
        Quota q = findByEntrepriseIdOrNull(entrepriseId);
        if (q == null) {
            q = Quota.builder().entrepriseId(entrepriseId).build();
        }
        q.setMaxEmployes(maxEmployes);
        q.setMaxSucursales(maxSucursales);
        q.setMaxStockageMo(maxStockageMo);
        q = quotaRepository.save(q);
        log.info("Quota enregistré (entreprise={}, maxEmployes={}, maxSucursales={}, maxStockageMo={})",
                entrepriseId, maxEmployes, maxSucursales, maxStockageMo);
        return q;
    }

    @Transactional
    public void delete(Long id) {
        Quota q = quotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quota introuvable : " + id));
        quotaRepository.delete(q);
    }
}