package com.codeintelligence.core;

import java.util.List;
import java.util.Map;

/**
 * Represents a semantic unit of code.
 * Sealed hierarchy to ensure we cover all types in pattern matching.
 */
public sealed interface CodeUnit permits 
    CodeUnit.ClassUnit, 
    CodeUnit.InterfaceUnit, 
    CodeUnit.MethodUnit,
    CodeUnit.RecordUnit {

    String id();
    String name();
    String content();
    Map<String, String> metadata();

    record ClassUnit(
        String id,
        String packageName,
        String name,
        String content,
        List<String> annotations,
        Map<String, String> metadata
    ) implements CodeUnit {}

    record InterfaceUnit(
        String id,
        String packageName,
        String name,
        String content,
        Map<String, String> metadata
    ) implements CodeUnit {}

    record RecordUnit(
        String id,
        String packageName,
        String name,
        String content,
        Map<String, String> metadata
    ) implements CodeUnit {}

    record MethodUnit(
        String id,
        String parentClassId,
        String signature,
        String name,
        String content,
        String returnType,
        List<String> parameters,
        Map<String, String> metadata
    ) implements CodeUnit {}
}
