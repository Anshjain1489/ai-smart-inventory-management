package com.anshtechsolution.smart_inventory.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.bootstrap-admin.enabled",
        havingValue = "true"
)
public class AdminBootstrap implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username}")
    private String username;

    @Value("${app.bootstrap-admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        if (username == null || username.isBlank()
                || password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "Bootstrap admin requires a username and a password "
                            + "of at least 12 characters");
        }

        if (appUserRepository.existsByUsername(username)) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        appUserRepository.save(admin);
    }
}