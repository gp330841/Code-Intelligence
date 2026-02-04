package com.codeintelligence.api;

import com.codeintelligence.ingestion.FileDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
@Slf4j
public class IngestionController {

    private final FileDiscoveryService fileDiscoveryService;
    private final com.codeintelligence.core.DependencyGraph dependencyGraph;

    @PostMapping
    public ResponseEntity<String> ingestProject(@RequestBody String path) {
        // Strip quotes if present
        String cleanPath = path.replace("\"", "").trim();
        log.info("Received ingestion request for path: {}", cleanPath);

        try {
            Path directory = Paths.get(cleanPath);
            if (!directory.toFile().exists() || !directory.toFile().isDirectory()) {
                return ResponseEntity.badRequest().body("Invalid directory path: " + cleanPath);
            }

            // Clear previous project graph to ensure fresh context
            dependencyGraph.clear();
            log.info("Dependency Graph cleared for new project.");

            int count = fileDiscoveryService.scanDirectory(directory);
            return ResponseEntity.ok("Ingestion started for " + count + " files in: " + cleanPath);

        } catch (Exception e) {
            log.error("Ingestion failed", e);
            return ResponseEntity.internalServerError().body("Error triggering ingestion: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> ingestUploadedProject(@RequestParam("files") org.springframework.web.multipart.MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files uploaded.");
        }
        
        try {
            // Create a temp directory for this upload
            Path tempDir = Files.createTempDirectory("ingest-" + System.currentTimeMillis());
            log.info("Created temp dir for upload: {}", tempDir);
            
            for (org.springframework.web.multipart.MultipartFile file : files) {
                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.isEmpty()) continue;
                
                // Handle relative paths (e.g., project/src/Main.java)
                Path targetPath = tempDir.resolve(originalName);
                Files.createDirectories(targetPath.getParent());
                
                file.transferTo(targetPath);
            }
            
            // Clear graph and scan
            dependencyGraph.clear();
            log.info("Scanning uploaded files in: {}", tempDir);
            
            int count = fileDiscoveryService.scanDirectory(tempDir);
            
            return ResponseEntity.ok("Successfully uploaded and ingested " + count + " files from project.");
            
        } catch (IOException e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }
}
