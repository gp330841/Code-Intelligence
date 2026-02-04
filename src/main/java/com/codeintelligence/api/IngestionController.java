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
}
