package com.server.filesharing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;

@Service
public class FileService {

    // Injects the path from your application.properties file
    @Value("${files.base-path.public}")
    private String publicPath;

    public List<String> getPublicFiles() {
        return getPublicFiles(null);
    }

    public List<String> getPublicFiles(String subPath) {
        try {
            Path basePath = Paths.get(publicPath);
            Path targetPath = (subPath != null && !subPath.isEmpty()) ? 
                basePath.resolve(subPath) : basePath;
            
            // Read the directory and get a list of file names
            try (var stream = Files.list(targetPath)) {
                return stream
                        .map(p -> {
                            String fileName = p.getFileName().toString();
                            // Add a prefix to distinguish folders from files
                            return Files.isDirectory(p) ? "FOLDER:" + fileName : fileName;
                        })
                        .toList();
            }
        } catch (IOException e) {
            // If the directory doesn't exist or there's an error, return an empty list
            System.err.println("Error reading public directory: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Value("${files.base-path.private}")
    private String privatePath;

    @Value("#{${private.code}}")
    private Map<String, String> privateCodes;

    public List<String> getPrivateFiles(String code, String subPath){
        String folderName = privateCodes.get(code);

        if (folderName == null) {
            return null;
        }

        try {
            Path basePath = Paths.get(privatePath, folderName);
            Path targetPath = (subPath != null && !subPath.isEmpty()) ? 
                basePath.resolve(subPath) : basePath;
            
            try (var stream = Files.list(targetPath)) {
                return stream
                        .map(p -> {
                            String fileName = p.getFileName().toString();
                            // Add a prefix to distinguish folders from files
                            return Files.isDirectory(p) ? "FOLDER:" + fileName : fileName;
                        })
                        .toList();
            }
        } catch (IOException e) {
            System.err.println("Error reading the path/ folder/ file does not exist: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // Overloaded method for backward compatibility
    public List<String> getPrivateFiles(String code){
        return getPrivateFiles(code, null);
    }

    public Resource loadFileAsResource(String type, String identifier, String filename) throws Exception {
        return loadFileAsResource(type, identifier, filename, null);
    }

    public Resource loadFileAsResource(String type, String identifier, String filename, String subPath) throws Exception {
        Path filePath;
        if ("public".equals(type)) {
            // For public files, the identifier is just a placeholder, we use the base public path
            Path basePath = Paths.get(publicPath);
            if (subPath != null && !subPath.isEmpty()) {
                basePath = basePath.resolve(subPath);
            }
            filePath = basePath.resolve(filename).normalize();
        } else if ("private".equals(type)) {
            // For private files, the identifier is the access code
            String folderName = privateCodes.get(identifier);
            if (folderName == null) {
                throw new RuntimeException("Invalid code.");
            }
            Path basePath = Paths.get(privatePath, folderName);
            if (subPath != null && !subPath.isEmpty()) {
                basePath = basePath.resolve(subPath);
            }
            filePath = basePath.resolve(filename).normalize();
        } else {
            throw new RuntimeException("Invalid file type specified.");
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists()) {
            return resource;
        } else {
            throw new Exception("File not found " + filename);
        }
    }

    public Resource createFolderZip(String code) throws Exception {
        return createFolderZip(code, null);
    }

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

    public Resource createPublicFolderZip(String subPath) throws Exception {
        Path basePath = Paths.get(publicPath);
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
            subPath.replace("/", "_") + ".zip" : "public_files.zip";

        return new ByteArrayResource(baos.toByteArray()) {
            @Override
            public String getFilename() {
                return zipFileName;
            }
        };
    }


}