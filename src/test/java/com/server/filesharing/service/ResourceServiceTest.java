package com.server.filesharing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @InjectMocks
    private ResourceService resourceService;

    private Path publicDir;
    private Path privateDir;

    @BeforeEach
    void setUp() throws IOException {
        publicDir = Files.createTempDirectory("public");
        privateDir = Files.createTempDirectory("private");
        Path testFolder = privateDir.resolve("testfolder");
        Files.createDirectories(testFolder);

        ReflectionTestUtils.setField(resourceService, "publicPath", publicDir.toString());
        ReflectionTestUtils.setField(resourceService, "privatePath", privateDir.toString());
        ReflectionTestUtils.setField(resourceService, "privateCodes", Map.of("testcode", "testfolder"));
        ReflectionTestUtils.setField(resourceService, "pathService", new PathService());
    }

    @Test
    void testLoadFileAsResource_Public() throws Exception {
        Files.createFile(publicDir.resolve("file1.txt"));

        Resource resource = resourceService.loadFileAsResource("public", null, "file1.txt", null);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("file1.txt", resource.getFilename());
    }

    @Test
    void testLoadFileAsResource_Private() throws Exception {
        Path testFolder = privateDir.resolve("testfolder");
        Files.createFile(testFolder.resolve("file1.txt"));

        Resource resource = resourceService.loadFileAsResource("private", "testcode", "file1.txt", null);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("file1.txt", resource.getFilename());
    }

    @Test
    void testLoadFileAsResource_InvalidCode() {
        assertThrows(RuntimeException.class, () -> {
            resourceService.loadFileAsResource("private", "wrongcode", "file1.txt", null);
        });
    }

    @Test
    void testLoadFileAsResource_FileNotFound() {
        assertThrows(Exception.class, () -> {
            resourceService.loadFileAsResource("public", null, "nonexistent.txt", null);
        });
    }

    @Test
    void testLoadFileAsResource_UnsafePath() {
        assertThrows(Exception.class, () -> {
            resourceService.loadFileAsResource("public", null, "../unsafe.txt", null);
        });
    }
}