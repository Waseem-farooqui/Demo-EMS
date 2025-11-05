package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    // Organization-aware methods for multi-tenancy
    Optional<User> findByUsernameAndOrganizationId(String username, Long organizationId);
    Optional<User> findByEmailAndOrganizationId(String email, Long organizationId);
    Boolean existsByUsernameAndOrganizationId(String username, Long organizationId);
    Boolean existsByEmailAndOrganizationId(String email, Long organizationId);

    List<User> findByOrganizationId(Long organizationId);
}

