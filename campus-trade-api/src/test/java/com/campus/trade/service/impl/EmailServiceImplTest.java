package com.campus.trade.service.impl;

import com.campus.trade.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {
    private final EmailServiceImpl service = new EmailServiceImpl();
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mailSender", mailSender);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "from", "noreply@example.test");
        ReflectionTestUtils.setField(service, "sendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "maxSendsPerHour", 5L);
        when(redisTemplate.opsForValue()).thenReturn(values);
    }

    @Test
    void rejectsInvalidEmailBeforeWritingToRedisOrSendingMail() {
        assertThrows(CustomException.class, () -> service.sendVerificationCode("not-an-email", "127.0.0.1"));
        verifyNoInteractions(redisTemplate, mailSender);
    }

    @Test
    void doesNotPersistUsableCodeWhenMailDeliveryFails() {
        when(values.setIfAbsent(startsWith("verification_cooldown:"), any(), anyLong(), any())).thenReturn(true);
        when(values.increment(startsWith("verification_rate:"))).thenReturn(1L);
        doThrow(new RuntimeException("smtp unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(RuntimeException.class, () -> service.sendVerificationCode("user@example.test", "127.0.0.1"));

        verify(values, never()).set(startsWith(EmailServiceImpl.VERIFICATION_CODE_KEY_PREFIX), any(), anyLong(), any());
    }
}
