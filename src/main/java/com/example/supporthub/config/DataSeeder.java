package com.example.supporthub.config;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a bootstrap ADMIN on startup if one does not already exist. Credentials are configured via
 * {@code app.seed.admin.username} / {@code app.seed.admin.password} (env SEED_ADMIN_USERNAME /
 * SEED_ADMIN_PASSWORD), defaulting to admin/admin for local development.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin.username}") String adminUsername,
                      @Value("${app.seed.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user '{}' already present; skipping seed", adminUsername);
            return;
        }
        User admin = new User(
                adminUsername,
                passwordEncoder.encode(adminPassword),
                "System Administrator",
                "admin@support-hub.local",
                Role.ADMIN,
                null);
        userRepository.save(admin);
        log.info("Seeded bootstrap ADMIN user '{}'", adminUsername);
    }
}
