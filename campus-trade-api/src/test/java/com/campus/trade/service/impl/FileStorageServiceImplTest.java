package com.campus.trade.service.impl;

import com.campus.trade.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {
    @TempDir Path uploadDir;

    @AfterEach
    void uploadsAreContainedInTemporaryDirectory() throws IOException {
        assertTrue(Files.list(uploadDir).allMatch(path -> path.getParent().equals(uploadDir)));
    }

    @Test
    void storesValidPngWithServerGeneratedName() {
        FileStorageServiceImpl service = new FileStorageServiceImpl(uploadDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00});

        String url = service.storeFile(file);

        assertTrue(url.matches("/uploads/[0-9a-f-]+\\.png"));
        assertEquals(1, uploadDir.toFile().listFiles().length);
    }

    @Test
    void rejectsTypeSpoofingAndPathTraversal() {
        FileStorageServiceImpl service = new FileStorageServiceImpl(uploadDir.toString());
        MockMultipartFile spoofed = new MockMultipartFile("file", "sample.png", "image/png", "not an image".getBytes());
        MockMultipartFile traversal = new MockMultipartFile("file", "../sample.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        assertThrows(CustomException.class, () -> service.storeFile(spoofed));
        assertThrows(CustomException.class, () -> service.storeFile(traversal));
    }

    @Test
    void rejectsUnsupportedMimeTypeAndOversizedFile() {
        FileStorageServiceImpl service = new FileStorageServiceImpl(uploadDir.toString());
        MockMultipartFile text = new MockMultipartFile("file", "sample.txt", "text/plain", "hello".getBytes());
        MockMultipartFile oversized = new MockMultipartFile("file", "sample.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

        assertThrows(CustomException.class, () -> service.storeFile(text));
        assertThrows(CustomException.class, () -> service.storeFile(oversized));
    }
}
