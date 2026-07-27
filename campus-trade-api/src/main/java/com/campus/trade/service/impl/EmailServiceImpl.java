package com.campus.trade.service.impl;

import com.campus.trade.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    public static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendVerificationCode(String to) {
        String code = generateVerificationCode();

        // 将验证码存入 Redis，有效期5分钟
        redisTemplate.opsForValue().set(VERIFICATION_CODE_KEY_PREFIX + to, code, 5, TimeUnit.MINUTES);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("GreenLoop - 您的注册验证码");
        message.setText("欢迎注册GreenLoop校园二手平台！您的验证码是：" + code + "，有效期为5分钟。");

        try {
            mailSender.send(message);
            log.info("已成功向 {} 发送验证码: {}", to, code);
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