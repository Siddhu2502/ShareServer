package com.server.filesharing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.server.filesharing.service.FileService;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import java.util.List;

@Controller
public class FileController {

    private final FileService fileService;

    private static final String FILES = "files";
    private static final String CURRENT_PATH = "currentPath";
    private static final String PARENT_PATH = "parentPath";
    private static final String FOLDER_CODE = "folderCode";


    // Use constructor injection to get the FileService instance
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/")
    public String homePage() {
        return "index";
    }

    // Public files handling
    @GetMapping("/public")
    public String listPublicFiles(@RequestParam(required = false) String subPath, Model model) {
        // Clean the subPath parameter to remove any URL encoding issues
        String cleanSubPath = (subPath != null) ? subPath.replace("?", "") : null;
        List<String> fileList = fileService.getPublicFiles(cleanSubPath);
        
        model.addAttribute(FILES, fileList);
        model.addAttribute(CURRENT_PATH, cleanSubPath != null ? cleanSubPath : "");
        model.addAttribute(PARENT_PATH, getParentPath(cleanSubPath));
        
        return "public-files";
    }

    @GetMapping("/browse-public")
    public String browsePublicSubfolder(@RequestParam String folder, Model model) {
        // Clean the folder parameter to remove any URL encoding issues
        String cleanFolder = folder.replace("?", "");
        List<String> fileList = fileService.getPublicFiles(cleanFolder);
        
        model.addAttribute(FILES, fileList);
        model.addAttribute(CURRENT_PATH, cleanFolder);
        model.addAttribute(PARENT_PATH, getParentPath(cleanFolder));
        return "public-files";
    }

    @GetMapping("/download-public-folder")
    public ResponseEntity<Resource> downloadPublicFolder(@RequestParam(required = false) String subPath) {
        try {
            String cleanSubPath = (subPath != null) ? subPath.replace("?", "") : null;
            Resource resource = fileService.createPublicFolderZip(cleanSubPath);
            String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Private files handling
    @PostMapping("/access-private")
    public String accessPrivateFiles(@RequestParam String code, 
                                     @RequestParam(required = false) String subPath,
                                     Model model, RedirectAttributes redirectAttributes) {
        // Clean the subPath parameter to remove any URL encoding issues
        String cleanSubPath = (subPath != null) ? subPath.replace("?", "") : null;
        List<String> fileList = fileService.getPrivateFiles(code, cleanSubPath);

        // If the service returns null, the code was invalid
        if (fileList == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid Access Code");
            return "redirect:/"; // Redirect back to the homepage
        }

        // If the code was valid, show the private files page
        model.addAttribute(FILES, fileList);
        model.addAttribute(FOLDER_CODE, code);
        model.addAttribute(CURRENT_PATH, cleanSubPath != null ? cleanSubPath : "");
        model.addAttribute(PARENT_PATH, getParentPath(cleanSubPath));
        return "private-files";
    }

    @GetMapping("/browse/{code}")
    public String browseSubfolder(@PathVariable String code,
                                  @RequestParam String folder,
                                  Model model) {
        // Clean the folder parameter to remove any URL encoding issues
        String cleanFolder = folder.replace("?", "");
        List<String> fileList = fileService.getPrivateFiles(code, cleanFolder);
        
        if (fileList == null) {
            return "redirect:/";
        }

        model.addAttribute(FILES, fileList);
        model.addAttribute(FOLDER_CODE, code);
        model.addAttribute(CURRENT_PATH, cleanFolder);
        model.addAttribute(PARENT_PATH, getParentPath(cleanFolder));
        return "private-files";
    }

    // Download methods
    @GetMapping("/download/{type}/{identifier}/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String type,
                                                 @PathVariable String identifier,
                                                 @PathVariable String filename,
                                                 @RequestParam(required = false) String subPath) {
        try {
            String cleanSubPath = (subPath != null) ? subPath.replace("?", "") : null;
            Resource resource = fileService.loadFileAsResource(type, identifier, filename, cleanSubPath);
            String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/download-folder/{code}")
    public ResponseEntity<Resource> downloadFolder(@PathVariable String code,
                                                   @RequestParam(required = false) String subPath) {
        try {
            String cleanSubPath = (subPath != null) ? subPath.replace("?", "") : null;
            Resource resource = fileService.createFolderZip(code, cleanSubPath);
            String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Helper method
    private String getParentPath(String currentPath) {
        if (currentPath == null || currentPath.isEmpty()) {
            return null;
        }
        int lastSlash = currentPath.lastIndexOf('/');
        return lastSlash > 0 ? currentPath.substring(0, lastSlash) : "";
    }
}
