package com.wifisense.service;

import com.wifisense.dto.CreateUserRequest;
import com.wifisense.model.User;
import com.wifisense.repository.UserRepository;
import com.wifisense.security.AccountProperties;
import com.wifisense.security.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(users, encoder, new PasswordPolicy(),
            new AccountProperties("admin", "Admin#2026a", 3));

    @Test
    void createsUserWithHashedPassword() {
        when(encoder.encode("Redes#2026a")).thenReturn("$2a$hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(new CreateUserRequest("jaider", "Redes#2026a", User.Role.ANALYST));

        assertThat(created.username()).isEqualTo("jaider");
        verify(users).save(argThat(user -> user.getPasswordHash().equals("$2a$hash")));
    }

    @Test
    void rejectsWeakPasswordWithAllReasons() {
        assertThatThrownBy(() -> service.create(new CreateUserRequest("jaider", "weak", User.Role.VIEWER)))
                .isInstanceOfSatisfying(InvalidAccountException.class, e -> assertThat(e.getErrors()).hasSize(4));
        verify(users, never()).save(any());
    }

    @Test
    void neverExceedsTheMaximumNumberOfUsers() {
        when(users.count()).thenReturn(3L);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("cuarto", "Redes#2026a", User.Role.VIEWER)))
                .isInstanceOf(UserLimitException.class)
                .hasMessageContaining("máximo de 3 usuarios");
    }

    @Test
    void adminCannotDeleteOrDemoteThemselves() {
        User admin = new User("admin", "admin@wifisense.local", "hash", User.Role.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(users.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.delete(1L, "admin")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.changeRole(1L, User.Role.VIEWER, "admin")).isInstanceOf(IllegalStateException.class);
    }
}
