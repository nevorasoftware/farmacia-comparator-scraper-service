package com.luppo.farmacia.scraper.repository;

import com.luppo.farmacia.scraper.entity.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    Optional<Pharmacy> findByCode(String code);
}
