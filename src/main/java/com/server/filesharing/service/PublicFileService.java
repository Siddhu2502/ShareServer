package com.server.filesharing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PublicFileService {

    @Value("${files.base-path.public}")
    private String publicPath;

    private final PathService pathService;

    public PublicFileService(PathService pathService) {
        this.pathService = pathService;
    }

    /**
     * Retrieves a list of public files and folders from a specified sub-path.
     * This method ensures the path is safe before listing the contents.
     * For example, it returns ["file1.txt", "FOLDER:folder1"] for a given path.
     *
     * @param subPath The sub-path to list files from.
     * @return A list of file and folder names.
     */
    public List<String> getPublicFiles(String subPath) {
        try {
            Path basePath = Paths.get(publicPath).toRealPath();
            String sanitizedSubPath = pathService.sanitizePath(subPath);
            Path targetPath;

            if (sanitizedSubPath != null && !sanitizedSubPath.isEmpty()) {
                targetPath = basePath.resolve(sanitizedSubPath).toRealPath();
                if (!pathService.isSafePath(basePath, targetPath)) {
                    System.err.println("Attempted access to unsafe path: " + sanitizedSubPath);
                    return Collections.emptyList();
                }
            } else {
                targetPath = basePath;
            }

            try (var stream = Files.list(targetPath)) {
                return stream
                        .map(p -> {
                            String fileName = p.getFileName().toString();
                            return Files.isDirectory(p) ? "FOLDER:" + fileName : fileName;
                        })
                        .toList();
            }
        } catch (IOException e) {
            System.err.println("Error reading public directory: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a list of public files and folders from the root public directory.
     * This method calls the overloaded getPublicFiles(String) with a null sub-path.
     * For example, it returns ["file1.txt", "FOLDER:folder1"] from the base public path.
     *
     * @return A list of file and folder names.
     */
    public List<String> getPublicFiles() {
        return getPublicFiles(null);
    }

    /**
     * Creates a zip archive of a public folder, including all its contents.
     * This method ensures the path is safe before creating the zip file.
     * For example, it generates a zip file for "/public/folder1".
     *
     * @param subPath The sub-path of the folder to be zipped.
     * @return A zip file as a Resource.
     * @throws Exception if the folder is not found or an error occurs during zipping.
     */
    public Resource createPublicFolderZip(String subPath) throws Exception {
        Path basePath = Paths.get(publicPath).toRealPath();
        String sanitizedSubPath = pathService.sanitizePath(subPath);
        Path folderPath;

        if (sanitizedSubPath != null && !sanitizedSubPath.isEmpty()) {
            folderPath = basePath.resolve(sanitizedSubPath).toRealPath();
            if (!pathService.isSafePath(basePath, folderPath)) {
                throw new RuntimeException("Attempted access to unsafe path: " + sanitizedSubPath);
            }
        } else {
            folderPath = basePath;
        }

        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new RuntimeException("Folder not found.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            try (var pathStream = Files.walk(folderPath)) {
                pathStream
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(folderPath.relativize(path).toString());
                            try {
                                zos.putNextEntry(zipEntry);
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                System.err.println("Error adding file to zip: " + e.getMessage());
                            }
                        });
            }
        }

        String zipFileName = (sanitizedSubPath != null && !sanitizedSubPath.isEmpty()) ?
                sanitizedSubPath.replace("/", "_") + ".zip" : "public_files.zip";

        return new ByteArrayResource(baos.toByteArray()) {
            @Override
            public String getFilename() {
                return zipFileName;
            }
        };
    }
}