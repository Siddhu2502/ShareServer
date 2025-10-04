package com.server.filesharing.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class PathService {

    /**
     * Sanitizes a given sub-path to prevent directory traversal and other security vulnerabilities.
     * This method removes leading/trailing slashes, replaces spaces with underscores, and filters out malicious patterns.
     * For example, it transforms "/example/path/../" into "example/path".
     *
     * @param subPath The path to be sanitized.
     * @return A sanitized path string, or null if the path is invalid.
     */
    public String sanitizePath(String subPath) {
        if (subPath == null || subPath.isEmpty()) {
            return "";
        }
        // Cleans the path by removing directory traversal attempts and normalizing slashes.
        String sanitizedString = subPath.replace("\\.\\.", "")
                .replace("//+", "/")
                .replace(" ", "_")
                .replace("^/+", "")
                .replace("/+$", "");

        // If the sanitized path is empty or contains only slashes, it's considered invalid.
        if (sanitizedString.isEmpty() || sanitizedString.matches("^[./\\\\]*$")) {
            return null;
        }
        return sanitizedString;
    }

    /**
     * Verifies if a target path is safely within a base directory, preventing directory traversal attacks.
     * This method checks if the canonical path of the target is prefixed by the canonical path of the base.
     * For example, it ensures that "/base/dir/target" is valid but "/base/../other" is not.
     *
     * @param basePath The base directory to check against.
     * @param targetPath The path to be validated.
     * @return True if the target path is safe, false otherwise.
     */
    public boolean isSafePath(Path basePath, Path targetPath) {
        try {
            // Checks if the real path of the target starts with the real path of the base directory.
            return targetPath.toRealPath().startsWith(basePath.toRealPath());
        } catch (IOException e) {
            return false;
        }
    }
}