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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PrivateFileService {

    @Value("${files.base-path.private}")
    private String privatePath;

    @Value("#{${private.code}}")
    private Map<String, String> privateCodes;

    private final PathService pathService;

    public PrivateFileService(PathService pathService) {
        this.pathService = pathService;
    }

    /**
     * Retrieves a list of private files and folders from a specified sub-path, protected by an access code.
     * This method validates the access code and ensures the path is safe before listing contents.
     * For example, it returns ["file1.txt", "FOLDER:folder1"] for a valid code and path.
     *
     * @param code The access code for the private folder.
     * @param subPath The sub-path to list files from.
     * @return A list of file and folder names.
     */
    public List<String> getPrivateFiles(String code, String subPath) {
        String folderName = privateCodes.get(code);
        if (folderName == null) {
            return Collections.emptyList();
        }

        try {
            Path basePath = Paths.get(privatePath, folderName);
            String sanitizedSubPath = pathService.sanitizePath(subPath);
            Path targetPath;

            if (sanitizedSubPath != null && !sanitizedSubPath.isEmpty()) {
                targetPath = basePath.resolve(sanitizedSubPath).toRealPath();
                if (!pathService.isSafePath(basePath, targetPath)) {
                    System.err.println("Attempted access to unsafe path: " + sanitizedSubPath);
                    return Collections.emptyList();
                }
            } else {
                targetPath = basePath.toRealPath();
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
            System.err.println("Error reading the path/ folder/ file does not exist: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a list of private files and folders from the root of a private directory.
     * This method calls the overloaded getPrivateFiles(String, String) with a null sub-path.
     * For example, it lists contents from the base private folder associated with the code.
     *
     * @param code The access code for the private folder.
     * @return A list of file and folder names.
     */
    public List<String> getPrivateFiles(String code) {
        return getPrivateFiles(code, null);
    }

    /**
     * Creates a zip archive of a private folder, protected by an access code.
     * This method validates the code and ensures the path is safe before creating the zip.
     * For example, it generates a zip file for a private folder specified by a code and sub-path.
     *
     * @param code The access code for the private folder.
     * @param subPath The sub-path of the folder to be zipped.
     * @return A zip file as a Resource.
     * @throws Exception if the folder is not found or an error occurs during zipping.
     */
    public Resource createFolderZip(String code, String subPath) throws Exception {
        String folderName = privateCodes.get(code);
        if (folderName == null) {
            throw new RuntimeException("Invalid code.");
        }

        Path basePath = Paths.get(privatePath, folderName);
        Path folderPath = (subPath != null && !subPath.isEmpty()) ?
                basePath.resolve(subPath) : basePath;

        if (!Files.exists(folderPath)) {
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

        String zipFileName = (subPath != null && !subPath.isEmpty()) ?
                subPath.replace("/", "_") + ".zip" : folderName + ".zip";

        return new ByteArrayResource(baos.toByteArray()) {
            @Override
            public String getFilename() {
                return zipFileName;
            }
        };
    }

    /**
     * Creates a zip archive of a private folder from its root directory.
     * This method calls the overloaded createFolderZip(String, String) with a null sub-path.
     * For example, it zips the entire private folder associated with the given code.
     *
     * @param code The access code for the private folder.
     * @return A zip file as a Resource.
     * @throws Exception if the folder is not found or an error occurs during zipping.
     */
    public Resource createFolderZip(String code) throws Exception {
        return createFolderZip(code, null);
    }
}