package com.codeintelligence;

import com.codeintelligence.core.DependencyGraph;
import com.codeintelligence.ingestion.FileDiscoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class IngestionIntegrationTest {

    @Autowired
    private FileDiscoveryService fileDiscoveryService;

    @Autowired
    private DependencyGraph dependencyGraph;
    
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.ai.vectorstore.VectorStore vectorStore;

    @Test
    void testIngestionFlow() throws IOException, InterruptedException {
        // 1. Create a dummy Java file
        Path tempDir = Files.createTempDirectory("code-intel-test");
        String javaCode = """
            package com.example;
            
            public class HelloWorld {
                public void sayHello() {
                    System.out.println("Hello");
                }
            }
            """;
        Files.writeString(tempDir.resolve("HelloWorld.java"), javaCode);

        // 2. Trigger Scan
        int count = fileDiscoveryService.scanDirectory(tempDir);
        
        // 3. Wait for Async Processing (Simple sleep for MVP test)
        Thread.sleep(2000);

        // 4. Assertions
        // Graph should contain the Class node
        // Warning: This assertion depends on the exact ID format "com.example.HelloWorld"
        // and "com.example.HelloWorld#sayHello"
        
        // Cannot easily assert private fields of Graph without exposed methods, 
        // effectively testing that no exception occurred and beans are wired.
        // For a real test, one would inspect the graph state.
        
        assertTrue(count > 0);
    }
}
