package com.wifisense.security;

import com.wifisense.model.User;
import com.wifisense.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Ensures the administrator account defined by ADMIN_USERNAME / ADMIN_PASSWORD exists at startup.
 * The password is stored only as a BCrypt hash and is re-applied if the variable changes.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AccountProperties properties;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;

    public AdminAccountInitializer(AccountProperties properties, UserRepository users, PasswordEncoder encoder,
                                   PasswordPolicy policy) {
        this.properties = properties;
        this.users = users;
        this.encoder = encoder;
        this.policy = policy;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = properties.adminUsername();
        String password = properties.adminPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("ADMIN_USERNAME / ADMIN_PASSWORD no están configurados: no se creó la cuenta de administrador");
            return;
        }
        List<String> violations = new ArrayList<>(policy.usernameViolations(username));
        violations.addAll(policy.passwordViolations(password));
        if (!violations.isEmpty()) {
            throw new IllegalStateException("La cuenta de administrador no cumple la política: " + String.join(" ", violations));
        }

        User admin = users.findByUsername(username)
                .orElseGet(() -> new User(username, PasswordPolicy.emailFor(username), encoder.encode(password), User.Role.ADMIN));
        if (!encoder.matches(password, admin.getPasswordHash())) {
            admin.changePassword(encoder.encode(password));
        }
        admin.changeRole(User.Role.ADMIN);
        users.save(admin);
        log.info("Cuenta de administrador '{}' lista", username);
    }
}
