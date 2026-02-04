package com.codeintelligence.core;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Thread-safe wrapper around JGraphT directed graph.
 * Nodes are CodeUnit IDs (String) to save memory, lookup map is separate.
 */
@Component
public class DependencyGraph {

    // Edge types could be enriched later (e.g. "CALLS", "IMPLEMENTS").
    private Graph<String, DependencyEdge> graph;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public DependencyGraph() {
        this.graph = new DirectedMultigraph<>(DependencyEdge.class);
    }

    public void addNode(String id) {
        lock.writeLock().lock();
        try {
            graph.addVertex(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addDependency(String fromId, String toId, DependencyType type) {
        lock.writeLock().lock();
        try {
            graph.addVertex(fromId);
            graph.addVertex(toId);
            graph.addEdge(fromId, toId, new DependencyEdge(type));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getIncoming(String nodeId) {
        lock.readLock().lock();
        try {
            if (!graph.containsVertex(nodeId)) return List.of();
            return graph.incomingEdgesOf(nodeId).stream()
                    .map(graph::getEdgeSource)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            // Create a new graph to clear state safely
            this.graph = new DirectedMultigraph<>(DependencyEdge.class);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getOutgoing(String nodeId) {
        lock.readLock().lock();
        try {
            if (!graph.containsVertex(nodeId)) return List.of();
            return graph.outgoingEdgesOf(nodeId).stream()
                    .map(graph::getEdgeTarget)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public enum DependencyType {
        CALLS, IMPLEMENTS, EXTENDS, USES, INJECTS
    }

    public static class DependencyEdge extends DefaultEdge {
        private final DependencyType type;

        public DependencyEdge(DependencyType type) {
            this.type = type;
        }

        public DependencyType getType() {
            return type;
        }
    }
}
