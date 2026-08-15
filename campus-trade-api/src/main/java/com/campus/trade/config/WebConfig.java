package com.campus.trade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Spring MVC 的额外配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadResourceLocation;

    public WebConfig(@Value("${file.upload-dir:uploads}") String uploadDir) {
        // 与 FileStorageServiceImpl 使用同一配置，避免测试环境写入
        // uploads-test、访问时却固定从 uploads 读取。
        this.uploadResourceLocation = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
    }

    /**
     * 配置静态资源映射。
     * 这是让图片能够通过URL被访问到的关键。
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadResourceLocation);
    }
}
