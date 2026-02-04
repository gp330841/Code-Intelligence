package com.codeintelligence.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDiscoveryService {

    private final ApplicationEventPublisher eventPublisher;

    public int scanDirectory(Path startPath) {
        log.info("Starting scan of directory: {}", startPath);
        AtomicInteger count = new AtomicInteger(0);

        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        eventPublisher.publishEvent(new SourceFileFoundEvent(file.toFile()));
                        count.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip hidden directories like .git
                    if (dir.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error scanning directory", e);
            throw new RuntimeException("Failed to scan directory", e);
        }

        log.info("Scan complete. Found {} Java files.", count.get());
        return count.get();
    }
}
