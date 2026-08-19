package com.campus.trade.service.impl;

import com.campus.trade.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import com.campus.trade.exception.CustomException;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    public static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";
    private static final String COOLDOWN_KEY_PREFIX = "verification_cooldown:";
    private static final String RATE_KEY_PREFIX = "verification_rate:";
    /** 限流计数窗口（1 小时），与 EXPIRE 保持一致。 */
    private static final long RATE_LIMIT_WINDOW_SECONDS = 3600L;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");

    /**
     * 原子限流脚本：INCR 计数，仅当计数为 1（首次）时设置过期，返回当前计数。
     * 把 INCR 与 EXPIRE 放进同一个 Lua 调用，避免"INCR 成功但 EXPIRE 前进程崩溃"
     * 导致计数 key 永不过期、来源被永久限流。
     */
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end "
                    + "return c",
            Long.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String from;
    @Value("${security.verification-code.send-cooldown-seconds:60}")
    private long sendCooldownSeconds;
    @Value("${security.verification-code.max-sends-per-hour:5}")
    private long maxSendsPerHour;

    @Override
    public void sendVerificationCode(String to, String clientIp) {
        if (to == null || !EMAIL_PATTERN.matcher(to.trim()).matches()) {
            throw new CustomException("邮箱格式不正确");
        }
        String email = to.trim().toLowerCase();
        String source = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
        if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(COOLDOWN_KEY_PREFIX + email, "1", sendCooldownSeconds, TimeUnit.SECONDS))) {
            throw new CustomException("验证码发送过于频繁，请稍后再试");
        }
        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(RATE_KEY_PREFIX + source),
                String.valueOf(RATE_LIMIT_WINDOW_SECONDS));
        if (count != null && count > maxSendsPerHour) {
            throw new CustomException("当前来源发送验证码次数过多，请稍后再试");
        }
        String code = generateVerificationCode();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("GreenLoop - 您的注册验证码");
        message.setText("欢迎注册GreenLoop校园二手平台！您的验证码是：" + code + "，有效期为5分钟。");

        try {
            mailSender.send(message);
            redisTemplate.opsForValue().set(VERIFICATION_CODE_KEY_PREFIX + email, code, 5, TimeUnit.MINUTES);
            log.info("已成功向 {} 发送验证码", email);
        } catch (Exception e) {
            log.error("向 {} 发送邮件失败", to, e);
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    private static final int SEED_LEN = 64;
    private static final int RANDOM_THRESHOLD = (int) Math.pow(2, 32);
    private static SecureRandom secureRandom;
    private static int randomCount = 0;

    private String generateVerificationCode() {
        initSecureRandom();
        refreshSeedIfNeeded();
        int code = secureRandom.nextInt(1000000);
        randomCount++;
        return String.format("%06d", code);
    }

    private synchronized void initSecureRandom() {
        if (secureRandom == null) {
            try {
                secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
                secureRandom.setSeed(secureRandom.generateSeed(SEED_LEN));
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                log.error("初始化安全随机数生成器失败", e);
                secureRandom = new SecureRandom();
            }
        }
    }

    private synchronized void refreshSeedIfNeeded() {
        if (randomCount >= RANDOM_THRESHOLD) {
            try {
                secureRandom.setSeed(secureRandom.generateSeed(SEED_LEN));
                randomCount = 0;
                log.info("安全随机数生成器已重新设置种子");
            } catch (Exception e) {
                log.error("重新设置随机数种子失败", e);
            }
        }
    }
}
