package com.clarix.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.Role;
import com.clarix.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByHospitalIdAndRoleIn(UUID hospitalId, List<Role> roles);
    List<User> findByHospitalIdAndRole(UUID hospitalId, Role role);
}
