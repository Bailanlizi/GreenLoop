package com.campus.trade.config;

import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CacheInitializerTest {
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void disabledBootstrapDoesNotLookUpOrCreateAdministrator() {
        CacheInitializer initializer = initializer(false, "", "", "");

        initializer.createBootstrapAdminIfEnabled();

        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void enabledBootstrapRejectsWeakOrIncompleteConfiguration() {
        CacheInitializer initializer = initializer(true, "admin", "weak", "admin@example.test");

        assertThrows(IllegalStateException.class, initializer::createBootstrapAdminIfEnabled);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void enabledBootstrapCreatesOnlyMissingConfiguredAdministrator() {
        CacheInitializer initializer = initializer(true, "first-admin", "Strong-Passphrase-2026!", "admin@example.test");
        when(userMapper.findByUsername("first-admin")).thenReturn(null);
        when(passwordEncoder.encode("Strong-Passphrase-2026!")).thenReturn("encoded");

        initializer.createBootstrapAdminIfEnabled();

        verify(userMapper).insertUserByAdmin(any(User.class));
        verify(userMapper).findByUsername("first-admin");
        verify(passwordEncoder).encode("Strong-Passphrase-2026!");
    }

    private CacheInitializer initializer(boolean enabled, String username, String password, String email) {
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptyList());
        return new CacheInitializer(cacheManager, userMapper, passwordEncoder, enabled, username, password, email);
    }
}
