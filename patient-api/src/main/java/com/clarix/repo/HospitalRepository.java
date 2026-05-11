package com.clarix.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.Hospital;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {
    List<Hospital> findByPartneredOrderByDistanceKmAsc(boolean partnered);
}
