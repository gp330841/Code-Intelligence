package com.codeintelligence.ingestion;

import com.codeintelligence.core.CodeUnit;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaParserService {

    private final ApplicationEventPublisher eventPublisher;
    private final JavaParser javaParser;

    // Constructor removed in favor of @RequiredArgsConstructor + @Bean config

    @Async
    @EventListener
    public void onFileFound(SourceFileFoundEvent event) {
        try {
            log.debug("Parsing file: {}", event.file().getName());
            ParseResult<CompilationUnit> result = javaParser.parse(event.file());
            
            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                processCompilationUnit(cu);
            } else {
                log.warn("Failed to parse file: {}", event.file().getName());
            }
        } catch (IOException e) {
            log.error("IO Exception parsing file: {}", event.file().getName(), e);
        }
    }

    private void processCompilationUnit(CompilationUnit cu) {
        String packageName = cu.getPackageDeclaration().map(p -> p.getName().asString()).orElse("");

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
            String className = c.getNameAsString();
            String id = packageName + "." + className; // Simple ID strategy

            if (c.isInterface()) {
                CodeUnit.InterfaceUnit interfaceUnit = new CodeUnit.InterfaceUnit(
                    id, packageName, className, c.toString(), Collections.emptyMap()
                );
                eventPublisher.publishEvent(new CodeParsedEvent(interfaceUnit));
            } else {
                CodeUnit.ClassUnit classUnit = new CodeUnit.ClassUnit(
                    id, packageName, className, c.toString(),
                    c.getAnnotations().stream().map(a -> a.getNameAsString()).toList(),
                    Collections.emptyMap()
                );
                eventPublisher.publishEvent(new CodeParsedEvent(classUnit));

                // Process methods
                c.getMethods().forEach(m -> {
                    String methodId = id + "#" + m.getNameAsString();
                    CodeUnit.MethodUnit methodUnit = new CodeUnit.MethodUnit(
                        methodId,
                        id,
                        m.getSignature().asString(),
                        m.getNameAsString(),
                        m.toString(),
                        m.getType().asString(),
                        m.getParameters().stream().map(p -> p.getType().asString() + " " + p.getNameAsString()).toList(),
                        Collections.emptyMap()
                    );
                    eventPublisher.publishEvent(new CodeParsedEvent(methodUnit));
                });
            }
        });
    }
}
