package com.server.filesharing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathServiceTest {

    private PathService pathService;

    @BeforeEach
    void setUp() {
        pathService = new PathService();
    }

    @Test
    void testSanitizePath() {
        assertEquals("", pathService.sanitizePath(null));
        assertEquals("", pathService.sanitizePath(""));
        assertEquals("path", pathService.sanitizePath("path"));
        assertEquals("path/to/file", pathService.sanitizePath("path/to/file"));
        assertEquals("path_to_file", pathService.sanitizePath("path to file"));
        assertEquals("path/to/file", pathService.sanitizePath("/path/to/file/"));
        assertNull(pathService.sanitizePath("../../../etc/passwd"));
        assertNull(pathService.sanitizePath("..\\..\\..\\etc\\passwd"));
    }

    @Test
    void testIsSafePath(@TempDir Path tempDir) throws IOException {
        Path basePath = tempDir.resolve("base");
        Files.createDirectories(basePath);

        Path safePath = basePath.resolve("safe/file");
        Files.createDirectories(safePath.getParent());
        Files.createFile(safePath);

        Path unsafePath = tempDir.resolve("unsafe/file");
        Files.createDirectories(unsafePath.getParent());
        Files.createFile(unsafePath);

        assertTrue(pathService.isSafePath(basePath, safePath));
        assertFalse(pathService.isSafePath(basePath, unsafePath));
        assertFalse(pathService.isSafePath(basePath, tempDir.resolve("non-existent")));
    }
}