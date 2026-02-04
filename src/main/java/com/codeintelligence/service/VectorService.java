package com.codeintelligence.service;

import com.codeintelligence.core.CodeUnit;
import com.codeintelligence.ingestion.CodeParsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorService {

    private final VectorStore vectorStore;

    @Async
    @EventListener
    public void onCodeParsed(CodeParsedEvent event) {
        CodeUnit unit = event.codeUnit();
        log.debug("Vectorizing code unit: {}", unit.id());

        // Create a Document from the CodeUnit
        // Metadata is crucial for filtering and context reconstruction
        Document document = new Document(
                unit.content(),
                Map.of(
                        "id", unit.id(),
                        "name", unit.name(),
                        "type", unit.getClass().getSimpleName()
                )
        );

        try {
            // Embed and Store
            vectorStore.add(List.of(document));
        } catch (Exception e) {
            log.error("Failed to vectorize unit: {}", unit.id(), e);
        }
    }
}
