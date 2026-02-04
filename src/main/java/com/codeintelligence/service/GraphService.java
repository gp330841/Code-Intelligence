package com.codeintelligence.service;

import com.codeintelligence.core.CodeUnit;
import com.codeintelligence.core.DependencyGraph;
import com.codeintelligence.ingestion.CodeParsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {

    private final DependencyGraph dependencyGraph;

    @EventListener
    public void onCodeParsed(CodeParsedEvent event) {
        CodeUnit unit = event.codeUnit();
        log.debug("Adding node to graph: {}", unit.id());
        
        // Add the node itself
        dependencyGraph.addNode(unit.id());

        // Basic Structural Edges (Method -> Class)
        if (unit instanceof CodeUnit.MethodUnit method) {
             // Link method to its parent class
             dependencyGraph.addDependency(method.parentClassId(), method.id(), DependencyGraph.DependencyType.USES);
        }
        
        // TODO: Advanced edge resolution (scanning imports, types, method calls)
        // This would require a second pass or richer event data from JavaParserService
    }
}
