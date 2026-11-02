
package com.anshtechsolution.smart_inventory.security;

import com.anshtechsolution.smart_inventory.security.dto.SignupRequest;
import com.anshtechsolution.smart_inventory.security.dto.SignupResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {

        String username = request.username().trim();

        if (appUserRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already taken"
            );
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.VIEWER);
        user.setEnabled(true);

        AppUser savedUser = appUserRepository.save(user);

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().name(),
                "User registered successfully"
        );
    }
}
