package com.server.filesharing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class ResourceService {

    @Value("${files.base-path.public}")
    private String publicPath;

    @Value("${files.base-path.private}")
    private String privatePath;

    @Value("#{${private.code}}")
    private Map<String, String> privateCodes;

    private final PathService pathService;

    public ResourceService(PathService pathService) {
        this.pathService = pathService;
    }

    /**
     * Loads a file as a resource, supporting both public and private files.
     * This method determines the file path based on the type and identifier, then returns it as a resource.
     * For example, it can load a public file from "/public/some-file.txt" or a private file from "/private/access-code/some-file.txt".
     *
     * @param type The type of file ("public" or "private").
     * @param identifier The access code for private files; can be null for public files.
     * @param filename The name of the file to load.
     * @param subPath An optional sub-path within the base directory.
     * @return The file as a Resource.
     * @throws Exception if the file is not found or the type is invalid.
     */
    public Resource loadFileAsResource(String type, String identifier, String filename, String subPath) throws Exception {
        Path basePath;
        if ("public".equals(type)) {
            basePath = Paths.get(publicPath);
        } else if ("private".equals(type)) {
            String folderName = privateCodes.get(identifier);
            if (folderName == null) {
                throw new RuntimeException("Invalid code.");
            }
            basePath = Paths.get(privatePath, folderName);
        } else {
            throw new RuntimeException("Invalid file type specified.");
        }

        String sanitizedSubPath = pathService.sanitizePath(subPath);
        String sanitizedFilename = pathService.sanitizePath(filename);

        if (sanitizedFilename == null || sanitizedFilename.isEmpty()) {
            throw new Exception("File not found " + filename);
        }

        Path targetPath = basePath;
        if (sanitizedSubPath != null && !sanitizedSubPath.isEmpty()) {
            targetPath = targetPath.resolve(sanitizedSubPath);
        }

        Path filePath = targetPath.resolve(sanitizedFilename).normalize();

        if (!pathService.isSafePath(basePath, filePath)) {
            throw new RuntimeException("Attempted access to unsafe path.");
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists()) {
            return resource;
        } else {
            throw new Exception("File not found " + filename);
        }
    }

    /**
     * Loads a file as a resource from the root directory (no sub-path).
     * This is an overloaded method that calls the main loadFileAsResource with a null sub-path.
     * For example, it simplifies loading files directly from the public or private root folders.
     *
     * @param type The type of file ("public" or "private").
     * @param identifier The access code for private files.
     * @param filename The name of the file to load.
     * @return The file as a Resource.
     * @throws Exception if the file is not found or the type is invalid.
     */
    public Resource loadFileAsResource(String type, String identifier, String filename) throws Exception {
        return loadFileAsResource(type, identifier, filename, null);
    }
}