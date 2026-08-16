package com.campus.trade.config;

import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CacheInitializer {

    private static final Logger log = LoggerFactory.getLogger(CacheInitializer.class);

    private final CacheManager cacheManager;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean bootstrapAdminEnabled;
    private final String bootstrapAdminUsername;
    private final String bootstrapAdminPassword;
    private final String bootstrapAdminEmail;

    public CacheInitializer(CacheManager cacheManager, UserMapper userMapper, PasswordEncoder passwordEncoder,
                            @org.springframework.beans.factory.annotation.Value("${security.bootstrap-admin.enabled:false}") boolean bootstrapAdminEnabled,
                            @org.springframework.beans.factory.annotation.Value("${security.bootstrap-admin.username:}") String bootstrapAdminUsername,
                            @org.springframework.beans.factory.annotation.Value("${security.bootstrap-admin.password:}") String bootstrapAdminPassword,
                            @org.springframework.beans.factory.annotation.Value("${security.bootstrap-admin.email:}") String bootstrapAdminEmail) {
        this.cacheManager = cacheManager;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminEnabled = bootstrapAdminEnabled;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(">>> [缓存初始化] 应用已启动，开始清空所有已知缓存...");

        cacheManager.getCacheNames().forEach(cacheName -> {
            log.info("--- 正在清空缓存: {}", cacheName);
            cacheManager.getCache(cacheName).clear();
        });

        log.info("<<< [缓存初始化] 所有缓存已成功清空。");

        createBootstrapAdminIfEnabled();
    }

    public void createBootstrapAdminIfEnabled() {
        if (!bootstrapAdminEnabled) {
            return;
        }
        if (bootstrapAdminUsername.isBlank() || bootstrapAdminEmail.isBlank() || bootstrapAdminPassword.length() < 12
                || "admin123".equals(bootstrapAdminPassword) || bootstrapAdminPassword.startsWith("<")) {
            throw new IllegalStateException("启用初始管理员时，必须配置非示例的强密码、用户名和邮箱");
        }
        User admin = userMapper.findByUsername(bootstrapAdminUsername);
        if (admin == null) {
            log.info("--- 创建配置的初始管理员账号: {}", bootstrapAdminUsername);
            User newAdmin = new User();
            newAdmin.setUsername(bootstrapAdminUsername);
            newAdmin.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
            newAdmin.setNickname("管理员");
            newAdmin.setRole("ADMIN");
            newAdmin.setCreditScore(100);
            newAdmin.setStatus(1);
            newAdmin.setEmail(bootstrapAdminEmail);
            newAdmin.setEmailVerified(true);

            userMapper.insertUserByAdmin(newAdmin);
            log.info("--- 初始管理员账号创建成功: {}", bootstrapAdminUsername);
        } else {
            log.info("--- 初始管理员账号已存在，跳过创建: {}", bootstrapAdminUsername);
        }
    }
}
