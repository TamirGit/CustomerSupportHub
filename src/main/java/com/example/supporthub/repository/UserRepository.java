package com.example.supporthub.repository;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Customers registered under a given agent. The {@code Agent_Id} path explicitly traverses the
     * {@code agent} association to its {@code id}; writing it as {@code AgentId} would clash with
     * the {@link User#getAgentId()} convenience getter and fail to resolve as a persistent attribute.
     */
    List<User> findByAgent_IdAndRole(Long agentId, Role role);

    /** All users of a given role (e.g. every CUSTOMER for an admin listing). */
    List<User> findByRole(Role role);
}
