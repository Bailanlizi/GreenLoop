package com.campus.trade.service.impl;

import com.campus.trade.dto.RegisterDTO;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.security.JwtUtil;
import com.campus.trade.service.FinanceService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceImplTest {
    @Test
    void normalizesEmailBeforeLookingUpAndConsumingVerificationCode() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        FinanceService financeService = mock(FinanceService.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("verification_code:heuok76apw@aamail.net")).thenReturn("123456");
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("1");
            return null;
        }).when(userMapper).insertUser(any(User.class));

        UserServiceImpl service = new UserServiceImpl(userMapper, passwordEncoder, authenticationManager,
                jwtUtil, redisTemplate, financeService);
        RegisterDTO request = new RegisterDTO();
        request.setUsername("test004");
        request.setNickname("004");
        request.setPassword("password123");
        request.setEmail("  heUOK76APW@aamail.net ");
        request.setVerificationCode("123456");

        service.register(request);

        verify(userMapper).findByEmail("heuok76apw@aamail.net");
        verify(values).get("verification_code:heuok76apw@aamail.net");
        verify(userMapper).insertUser(argThat(user -> "heuok76apw@aamail.net".equals(user.getEmail())));
        verify(redisTemplate).delete("verification_code:heuok76apw@aamail.net");
        verify(financeService).ensureAccount("1");
    }
}
