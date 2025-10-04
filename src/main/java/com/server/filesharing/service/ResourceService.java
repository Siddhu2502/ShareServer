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