package com.campus.trade.config;

import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BootstrapAdminInitializerTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void disabledBootstrapDoesNotLookUpOrCreateAdministrator() {
        BootstrapAdminInitializer initializer = initializer(false, "", "", "");

        initializer.createBootstrapAdminIfEnabled();

        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void enabledBootstrapRejectsWeakOrIncompleteConfiguration() {
        BootstrapAdminInitializer initializer = initializer(true, "admin", "weak", "admin@example.test");

        assertThrows(IllegalStateException.class, initializer::createBootstrapAdminIfEnabled);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void enabledBootstrapCreatesOnlyMissingConfiguredAdministrator() {
        BootstrapAdminInitializer initializer = initializer(true, "first-admin", "Strong-Passphrase-2026!", "admin@example.test");
        when(userMapper.findByUsername("first-admin")).thenReturn(null);
        when(passwordEncoder.encode("Strong-Passphrase-2026!")).thenReturn("encoded");

        initializer.createBootstrapAdminIfEnabled();

        verify(userMapper).insertUserByAdmin(any(User.class));
        verify(userMapper).findByUsername("first-admin");
        verify(passwordEncoder).encode("Strong-Passphrase-2026!");
    }

    private BootstrapAdminInitializer initializer(boolean enabled, String username, String password, String email) {
        return new BootstrapAdminInitializer(userMapper, passwordEncoder, enabled, username, password, email);
    }
}
