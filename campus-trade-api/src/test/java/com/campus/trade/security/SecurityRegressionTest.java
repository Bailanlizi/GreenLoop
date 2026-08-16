package com.campus.trade.security;

import com.campus.trade.controller.AdminController;
import com.campus.trade.service.AddressService;
import com.campus.trade.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=test-only-secret-that-is-long-enough-for-hs512-signing-tests-2026",
        "jwt.expiration=60000",
        "security.cors.allowed-origins=http://localhost:5173,http://localhost:8000",
        "orders.payment-expiration-enabled=false",
        "spring.mail.host=localhost",
        "spring.datasource.username=test",
        "spring.datasource.password=test"
})
@AutoConfigureMockMvc
class SecurityRegressionTest {
    private static final String OLD_SECRET = "old-secret-that-must-not-be-accepted-after-immediate-jwt-rotation-2025";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @MockBean private UserService userService;
    @MockBean private AddressService addressService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private AuthenticatedUser user;
    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {
        user = principal("user", "1", false, true);
        admin = principal("admin", "2", true, true);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(user);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(admin);
    }

    @Test
    void rejectsUnauthenticatedAndInvalidOrOldJwtRequests() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/files/upload")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + oldToken("user")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scopesAddressMutationToAuthenticatedUserRatherThanPathInput() throws Exception {
        mockMvc.perform(put("/me/addresses/999")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(user))
                        .contentType("application/json")
                        .content("{\"recipientName\":\"Receiver\",\"phone\":\"13800138000\",\"province\":\"A\",\"city\":\"B\",\"district\":\"C\",\"detailedAddress\":\"Street\"}"))
                .andExpect(status().isOk());

        verify(addressService).updateAddress(org.mockito.ArgumentMatchers.eq(999L), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsRegularUserAndDisabledUserWithOtherwiseValidTokens() throws Exception {
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + jwtUtil.generateToken(user)))
                .andExpect(status().isForbidden());

        AuthenticatedUser disabled = principal("disabled", "3", false, false);
        when(userDetailsService.loadUserByUsername("disabled")).thenReturn(disabled);
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + jwtUtil.generateToken(disabled)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAdminAndEnforcesCorsOriginAllowlist() throws Exception {
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + jwtUtil.generateToken(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(options("/admin/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/admin/users")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private AuthenticatedUser principal(String username, String userId, boolean isAdmin, boolean enabled) {
        return new AuthenticatedUser(username, "encoded", enabled,
                Collections.singletonList(new SimpleGrantedAuthority(isAdmin ? "ROLE_ADMIN" : "ROLE_USER")),
                userId, username, null);
    }

    private String oldToken(String username) {
        return Jwts.builder().setSubject(username).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SignatureAlgorithm.HS512, OLD_SECRET).compact();
    }
}
