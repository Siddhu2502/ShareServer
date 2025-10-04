package com.server.filesharing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicFileServiceTest {

    @Mock
    private PathService pathService;

    @InjectMocks
    private PublicFileService publicFileService;

    private Path publicDir;

    @BeforeEach
    void setUp() throws IOException {
        publicDir = Files.createTempDirectory("public");
        ReflectionTestUtils.setField(publicFileService, "publicPath", publicDir.toString());
    }

    @Test
    void testGetPublicFiles() throws IOException {
        Files.createFile(publicDir.resolve("file1.txt"));
        Files.createDirectory(publicDir.resolve("folder1"));

        when(pathService.sanitizePath(null)).thenReturn("");

        List<String> files = publicFileService.getPublicFiles();
        assertEquals(2, files.size());
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("FOLDER:folder1"));
    }

    @Test
    void testCreatePublicFolderZip() throws Exception {
        Files.createFile(publicDir.resolve("file1.txt"));
        Files.createDirectory(publicDir.resolve("folder1"));
        Files.createFile(publicDir.resolve("folder1/file2.txt"));

        when(pathService.sanitizePath(null)).thenReturn("");

        Resource resource = publicFileService.createPublicFolderZip(null);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.contentLength() > 0);
        assertEquals("public_files.zip", resource.getFilename());
    }
}