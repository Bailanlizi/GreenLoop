package com.campus.trade.service.impl;

import com.campus.trade.exception.CustomException;
import com.campus.trade.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Set;
import org.springframework.util.unit.DataSize;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private final long maxUploadBytes;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    /**
     * 构造函数支持通过配置文件动态指定上传目录，兼容本地和生产环境。
     */
    @Autowired
    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir,
                                  @Value("${file.max-upload-size:5MB}") String maxUploadSize) {
        // 支持相对路径和绝对路径
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new CustomException("无法创建用于存储上传文件的目录！ " + ex.getMessage());
        }
        this.maxUploadBytes = DataSize.parse(maxUploadSize).toBytes();
    }

    FileStorageServiceImpl(String uploadDir) {
        this(uploadDir, "5MB");
    }

    @Override
    public String storeFile(MultipartFile file) {
        // 清理并获取原始文件名
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new CustomException("无法存储空文件。");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new CustomException("上传文件超过大小限制");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new CustomException("仅支持 JPEG、PNG 或 WebP 图片");
        }
        // 防止路径遍历攻击
        if (originalFileName.contains("..")) {
            throw new CustomException("文件名包含无效的路径序列 " + originalFileName);
        }

        validateImageSignature(file);

        try {
            // 获取文件扩展名
            String fileExtension = "";
            int lastDot = originalFileName.lastIndexOf('.');
            if (lastDot >= 0) {
                fileExtension = originalFileName.substring(lastDot);
            }
            // 使用UUID生成唯一文件名，防止重名
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // 确定最终保存的目标路径
            Path targetLocation = this.fileStorageLocation.resolve(newFileName).normalize();
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new CustomException("文件存储路径无效");
            }

            // 使用 try-with-resources 确保输入流被自动关闭
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // 返回一个可以通过Web访问的相对URL路径
            return "/uploads/" + newFileName;

        } catch (IOException ex) {
            throw new CustomException("无法存储文件 " + originalFileName + "。请重试！错误: " + ex.getMessage());
        }
    }

    private void validateImageSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            boolean png = header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            boolean jpeg = header.length >= 3 && header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;
            boolean webp = header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            if (!(png || jpeg || webp)) {
                throw new CustomException("上传文件内容不是合法图片");
            }
        } catch (IOException ex) {
            throw new CustomException("无法读取上传文件");
        }
    }
}
