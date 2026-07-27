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

    public CacheInitializer(CacheManager cacheManager, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.cacheManager = cacheManager;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(">>> [缓存初始化] 应用已启动，开始清空所有已知缓存...");

        cacheManager.getCacheNames().forEach(cacheName -> {
            log.info("--- 正在清空缓存: {}", cacheName);
            cacheManager.getCache(cacheName).clear();
        });

        log.info("<<< [缓存初始化] 所有缓存已成功清空。");

        log.info(">>> [管理员初始化] 检查是否存在默认管理员账号...");
        createDefaultAdminIfNotExists();
    }

    private void createDefaultAdminIfNotExists() {
        User admin = userMapper.findByUsername("admin");
        if (admin == null) {
            log.info("--- 创建默认管理员账号...");
            User newAdmin = new User();
            newAdmin.setUsername("admin");
            newAdmin.setPassword(passwordEncoder.encode("admin123"));
            newAdmin.setNickname("管理员");
            newAdmin.setRole("ADMIN");
            newAdmin.setCreditScore(100);
            newAdmin.setStatus(1);
            newAdmin.setEmail("admin@example.com");
            newAdmin.setEmailVerified(true);

            userMapper.insertUserByAdmin(newAdmin);
            log.info("--- 默认管理员账号创建成功！用户名: admin, 密码: admin123");
        } else {
            log.info("--- 默认管理员账号已存在，跳过创建。");
        }
    }
}
