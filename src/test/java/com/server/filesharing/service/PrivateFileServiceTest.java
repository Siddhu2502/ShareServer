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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivateFileServiceTest {

    @Mock
    private PathService pathService;

    @InjectMocks
    private PrivateFileService privateFileService;

    private Path privateDir;

    @BeforeEach
    void setUp() throws IOException {
        privateDir = Files.createTempDirectory("private");
        Path testFolder = privateDir.resolve("testfolder");
        Files.createDirectories(testFolder);
        ReflectionTestUtils.setField(privateFileService, "privatePath", privateDir.toString());
        ReflectionTestUtils.setField(privateFileService, "privateCodes", Map.of("testcode", "testfolder"));
    }

    @Test
    void testGetPrivateFiles() throws IOException {
        Path testFolder = privateDir.resolve("testfolder");
        Files.createFile(testFolder.resolve("file1.txt"));
        Files.createDirectory(testFolder.resolve("folder1"));

        when(pathService.sanitizePath(null)).thenReturn("");

        List<String> files = privateFileService.getPrivateFiles("testcode", null);
        assertEquals(2, files.size());
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("FOLDER:folder1"));
    }

    @Test
    void testGetPrivateFiles_InvalidCode() {
        List<String> files = privateFileService.getPrivateFiles("wrongcode", null);
        assertTrue(files.isEmpty());
    }

    @Test
    void testCreateFolderZip() throws Exception {
        Path testFolder = privateDir.resolve("testfolder");
        Files.createFile(testFolder.resolve("file1.txt"));
        Files.createDirectory(testFolder.resolve("folder1"));
        Files.createFile(testFolder.resolve("folder1/file2.txt"));

        when(pathService.sanitizePath(null)).thenReturn("");

        Resource resource = privateFileService.createFolderZip("testcode", null);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.contentLength() > 0);
        assertEquals("testfolder.zip", resource.getFilename());
    }

    @Test
    void testCreateFolderZip_InvalidCode() {
        assertThrows(RuntimeException.class, () -> {
            privateFileService.createFolderZip("wrongcode", null);
        });
    }
}