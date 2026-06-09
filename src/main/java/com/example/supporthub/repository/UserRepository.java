package com.example.supporthub.repository;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Customers registered under a given agent. */
    List<User> findByAgentIdAndRole(Long agentId, Role role);
}
