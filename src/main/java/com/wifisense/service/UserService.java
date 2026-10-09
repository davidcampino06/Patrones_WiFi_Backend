package com.wifisense.service;

import com.wifisense.dto.CreateUserRequest;
import com.wifisense.dto.UserResponse;
import com.wifisense.model.User;
import com.wifisense.repository.UserRepository;
import com.wifisense.security.AccountProperties;
import com.wifisense.security.PasswordPolicy;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Accounts are created only by an administrator, up to a fixed maximum (there is no self-registration). */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy policy;
    private final AccountProperties accounts;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, PasswordPolicy policy,
                       AccountProperties accounts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.policy = policy;
        this.accounts = accounts;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        List<String> errors = new ArrayList<>(policy.usernameViolations(request.username()));
        errors.addAll(policy.passwordViolations(request.password()));
        if (!errors.isEmpty()) {
            throw new InvalidAccountException(errors);
        }
        if (users.count() >= accounts.maxUsers()) {
            throw new UserLimitException(accounts.maxUsers());
        }
        if (users.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Ese nombre de usuario ya existe.");
        }
        User user = new User(request.username(), request.username() + "@wifisense.local",
                passwordEncoder.encode(request.password()), request.role());
        return UserResponse.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return users.findByUsername(username).map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("el usuario", username));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll(Sort.by("username")).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse changeRole(Long id, User.Role role, String currentUsername) {
        User user = find(id);
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("No puedes cambiar tu propio rol.");
        }
        user.changeRole(role);
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        User user = find(id);
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("No puedes eliminar tu propia cuenta.");
        }
        users.delete(user);
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("el usuario", id));
    }
}
