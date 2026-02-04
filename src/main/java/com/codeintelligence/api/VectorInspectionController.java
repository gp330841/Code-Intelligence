package com.codeintelligence.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/vectors")
@RequiredArgsConstructor
@Slf4j
public class VectorInspectionController {

    @Value("${spring.ai.vectorstore.chroma.client.host}")
    private String chromaHost;

    @Value("${spring.ai.vectorstore.chroma.client.port}")
    private int chromaPort;

    @GetMapping
    public ResponseEntity<?> getAllVectors() {
        String chromaUrl = chromaHost + ":" + chromaPort;
        // Default Spring AI collection name is "SpringAiCollection"
        // Using ChromaDB API to fetch data: /api/v1/collections/{collection_id}/get
        // note: we first need the UUID, but commonly we can try list or get by name if supported.
        // Actually, Spring AI uses `SpringAiCollection`.
        
        RestClient client = RestClient.create();
        
        try {
            // 1. Get Collection ID for "SpringAiCollection"
            // Chroma API: GET /api/v1/collections
            // We'll just proxy the list for now or try to get the specific one.
            // A simple implementation: Just return the raw collections list so the UI can decide what to show
            // Or we can try to be smarter. Let's just return the raw dump for the UI to parse.
            
            // Getting the actual content (get)
            // Ideally: GET /api/v1/collections -> find ID -> GET /api/v1/collections/{ID}/get
            
            // For MVP, let's just return the collections list to verify connectivity
            // User asked for "all data".
            
            // Let's rely on the fact that we know the collection name if possible, 
            // but the API requires UUID. 
            
            // Let's fetch all collections first.
            ResponseEntity<Object> collections = client.get()
                    .uri(chromaUrl + "/api/v1/collections")
                    .retrieve()
                    .toEntity(Object.class);
            
            return ResponseEntity.ok(collections.getBody());
            
        } catch (Exception e) {
            log.error("Failed to fetch vectors from Chroma", e);
            return ResponseEntity.internalServerError().body("Failed to connect to ChromaDB at " + chromaUrl + ": " + e.getMessage());
        }
    }
    
    @GetMapping("/content")
    public ResponseEntity<?> getCollectionContent() {
         String chromaUrl = chromaHost + ":" + chromaPort;
         RestClient client = RestClient.create();
         
         try {
             // Fetch collections to find the ID of 'SpringAiCollection'
             // This is a bit hacky with Map<String, Object> but robust enough for a dev tool.
             var collections = client.get()
                    .uri(chromaUrl + "/api/v1/collections")
                    .retrieve()
                    .body(java.util.List.class); // List of Maps
             
             if (collections == null || collections.isEmpty()) {
                 return ResponseEntity.ok("No collections found.");
             }
             
             // Find ID
             String collectionId = null;
             for (Object o : collections) {
                 if (o instanceof Map) {
                     Map<?, ?> map = (Map<?, ?>) o;
                     if ("SpringAiCollection".equals(map.get("name"))) {
                         collectionId = (String) map.get("id");
                         break;
                     }
                 }
             }
             
             if (collectionId == null) {
                  // Fallback to first
                  Map<?,?> first = (Map<?,?>) collections.get(0);
                  collectionId = (String) first.get("id");
             }
             
             // 2. Fetch Content
             // POST /api/v1/collections/{id}/get
             log.info("Fetching content for collection: {} from {}", collectionId, chromaUrl);
             
             try {
                // explicit include to be safe
                Map<String, Object> body = Map.of(
                    "limit", 100,
                    "include", java.util.List.of("documents", "metadatas")
                );
             
                Map<String, Object> response = client.post()
                    .uri(chromaUrl + "/api/v1/collections/" + collectionId + "/get")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
                
                log.info("Fetch success. Keys: {}", response != null ? response.keySet() : "null");
                return ResponseEntity.ok(response);
             
             } catch (org.springframework.web.client.RestClientResponseException e) {
                 log.error("ChromaDB Error: {} - Body: {}", e.getStatusText(), e.getResponseBodyAsString());
                 throw e;
             } catch (Exception e) {
                 log.error("Error calling ChromaDB GET: {}", e.getMessage());
                 throw e;
             }
             
         } catch (Exception e) {
              log.error("Failed to fetch content from Chroma at {}", chromaUrl, e);
              return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
         }
    }
}
