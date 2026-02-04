package com.codeintelligence.api;

import com.codeintelligence.core.DependencyGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    private final DependencyGraph dependencyGraph;

    @PostMapping
    public String chat(@RequestBody String query) {
        log.info("Received chat query: {}", query);

        // 1. Semantic Search (Find relevant entry points)
        List<Document> semanticHits = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).build()
        );

        if (semanticHits.isEmpty()) {
            return "No relevant code found in the workspace to answer this question.";
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Context from Source Code:\n");

        // 2. Graph Expansion (Traverse dependencies)
        for (Document doc : semanticHits) {
            String nodeId = (String) doc.getMetadata().get("id");
            contextBuilder.append("\n--- Found Match: ").append(nodeId).append(" ---\n");
            contextBuilder.append(doc.getContent()).append("\n");

            // Look up connected nodes in the graph
            List<String> dependencies = dependencyGraph.getOutgoing(nodeId);
            if (!dependencies.isEmpty()) {
                contextBuilder.append("\n  [Dependencies]: ").append(String.join(", ", dependencies)).append("\n");
                // In a real app, we would fetch the content of these dependencies too.
                // For now, we list them to give the LLM structural awareness.
            }
            
            List<String> callers = dependencyGraph.getIncoming(nodeId);
            if (!callers.isEmpty()) {
                contextBuilder.append("\n  [Called By]: ").append(String.join(", ", callers)).append("\n");
            }
        }

        // 3. Generation (RAG)
        String systemPrompt = """
            You are a specialized Code Intelligence Agent.
            Your analysis must be based STRICTLY on the provided 'Code Context' (Source Code snippets and Dependency Graph relationships).
            
            Guidelines:
            1. Trace execution flow using the provided code and dependency links.
            2. If the user asks about a concept not present in the context, explicitly state: "I cannot find this in the ingested codebase."
            3. Do not Hallucinate or assume code that isn't shown.
            4. Keep answers concise and technical.
            """;

        String userPrompt = "Question: " + query + "\n\n" + contextBuilder.toString();

        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
