package com.campus.trade.config;

import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动后按配置引导创建初始管理员账号。
 *
 * 历史版本曾在此处清空全部 Redis 缓存（CacheManager.clear），
 * 作为"缓存无 TTL、靠重启保证一致性"的补救。引入 per-cache TTL 的
 * 自定义 CacheManager 后该补丁已无必要，故移除，本类只保留管理员引导。
 */
@Component
public class BootstrapAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean bootstrapAdminEnabled;
    private final String bootstrapAdminUsername;
    private final String bootstrapAdminPassword;
    private final String bootstrapAdminEmail;

    public BootstrapAdminInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder,
                                     @Value("${security.bootstrap-admin.enabled:false}") boolean bootstrapAdminEnabled,
                                     @Value("${security.bootstrap-admin.username:}") String bootstrapAdminUsername,
                                     @Value("${security.bootstrap-admin.password:}") String bootstrapAdminPassword,
                                     @Value("${security.bootstrap-admin.email:}") String bootstrapAdminEmail) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminEnabled = bootstrapAdminEnabled;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
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
