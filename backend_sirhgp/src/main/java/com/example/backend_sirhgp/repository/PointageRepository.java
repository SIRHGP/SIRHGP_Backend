package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointageRepository extends JpaRepository<Pointage, Long> {

    Optional<Pointage> findByEmployeIdAndDate(Long employeId, LocalDate date);

    List<Pointage> findByEmployeIdAndDateBetweenOrderByDateDesc(Long employeId, LocalDate debut, LocalDate fin);

    List<Pointage> findByEmployeIdAndDateBetweenOrderByDateAsc(Long employeId, LocalDate debut, LocalDate fin);
}
