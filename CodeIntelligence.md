# SpringSeek: Universal Spring Boot Code Intelligence Agent - PRD

**Version:** 1.0  
**Status:** Approved (Principal Engineer)  
**Date:** January 24, 2026

---

## 1. Problem Statement
Developers working on Spring Boot applications often struggle to trace execution flows due to the framework's decoupled architecture (Interfaces vs. Implementations), dependency injection magic (`@Autowired`), and scattered configuration (`application.yml`). 

Traditional RAG (Retrieval-Augmented Generation) tools fail here because they treat code as unstructured text, missing the critical relationships between components. We need a native **Spring Boot-powered Agent** (eating our own dog food) that parses code into a **dependency graph** to provide accurate, deterministic explanations of complex logic flows using the Spring ecosystem.

## 2. Objectives
* **Build a Mental Model:** Create an agent that understands the specific wiring of a Spring Boot project, not just the text content.
* **Automated Flow Tracing:** Enable users to ask "trace the flow" questions and receive a response that follows the execution path across multiple files.
* **Privacy First:** Run entirely on local infrastructure (Ollama + Local Vector Store) to ensure proprietary code never leaves the machine.

## 3. User Personas
* **The Forensic Developer:** Needs to understand a legacy module where the original authors have left.
* **The Architect:** Needs to visualize dependencies and identify tight coupling in the codebase.
* **The Onboarder:** Needs to understand high-level flows (e.g., "Order Creation") without manually navigating 50+ files.

---

## 4. Core Feature Requirements

| Feature | Priority | Description | Technical Requirement |
| :--- | :--- | :--- | :--- |
| **Source Ingestion** | **P0** | Recursively walk a directory to ingest `.java`, `.xml` (POM), and `.yml/.properties` files. | Java NIO `Files.walk` |
| **Incremental Indexing**| **P1** | Watch for file changes and trigger re-indexing only for affecting sub-graphs. | `WatchService` + Checksum |
| **AST-Aware Parsing** | **P0** | Parse code into semantic chunks (Class, Method) rather than arbitrary text splits. Handle Java 17-21 syntax. | `JavaParser` (JavaSymbolSolver) |
| **Robust Symbol Resolution** | **P0** | Map Interfaces to Implementations using heuristic Type Solving when dependencies are missing (Source-only analysis). | `JavaParser` + Fallback Logic |
| **Hybrid Indexing** | **P1** | Combine Vector Search (for meaning) with Graph Traversal (for structure). | `Spring AI` + `JGraphT` |
| **Configuration Parsing**| **P1** | Read and index `application.yml` to resolve `@Value` properties and active profiles. | `SnakeYAML` / Spring Boot Config Support |
| **Dynamic Chat** | **P1** | A RAG interface that retrieves the *entire call chain* for a requested flow, not just single files. | `Spring AI` + `Ollama` |
| **Observability** | **P2** | Track token usage, ingestion latency, and graph memory footprint. | Spring Boot Actuator + Micrometer |

---

## 5. Technical Architecture & Stack

### 5.1 Technology Stack
* **Language:** Java 21 (LTS) - Leveraging Virtual Threads for concurrent ingestion.
* **Framework:** Spring Boot 3.3+ (Web, AOT support).
* **AI Framework:** **Spring AI** (0.8.0+) - Unified API for LLMs and Vector Stores.
* **LLM:** Ollama (running `deepseek-r1` or `codellama`).
* **Vector Store:** ChromaDB (via Spring AI's ChromaVectorStore) or PGVector.
* **Graph Engine:** **JGraphT** (In-memory dependency graph) or Guava Graph.
* **Parser:** **JavaParser** (com.github.javaparser) for native AST analysis and Type Solving.

### 5.2 The "SpringSeek" Pipeline

#### **Stage 1: Deep Ingestion (Event-Driven Architecture)**
*Architecture Note: We use Spring's `ApplicationEventPublisher` to decouple scanning from parsing and embedding. This allows for better error containment and progress tracking.*

1.  **File Scanning & Change Detection:** 
    * `FileDiscoveryService` scans directory or receives `FileChangeEvent`.
    * Publishes `SourceFileFoundEvent`.
2.  **AST Analysis (Async Listener):**
    * `JavaParserListener` consumes events.
    * **Heuristic Type Solving:** Since we may not have the full classpath (user might not have run `mvn install`), we use a *CombinedTypeSolver* (Reflection + JavaParserTypeSolver) with fallback to simple name matching if resolution fails.
    * Extract **Nodes**: Classes, Records, Methods, Fields.
    * Extract **Edges**: `implements`, `extends`, `method calls`, `@Autowired` injections.
    * Publishes `CompilationUnitParsedEvent`.
3.  **Graph Construction (GraphService):**
    * Updates the `JGraphT` directed graph in a thread-safe manner.
    * **Resolution Strategy:** 
        * *Exact Match:* Full FQN match (best).
        * *Fuzzy Match:* Interface name + Implementation name heuristic (e.g., `IPayment` -> `StripePayment`).
    * Link Bean Definitions: Connect `@Service` logic to `@Controller` endpoints.
4.  **Vectorization (EmbeddingService):**
    * Consumes `CompilationUnitParsedEvent`.
    * Chunk method bodies and class definitions (Smart Chunking).
    * Embed using Ollama (`nomic-embed-text`) via `Spring AI EmbeddingClient`.
    * Store in Vector DB with metadata.

#### **Stage 2: The Retrieval Loop (The Tracer)**
When a user asks: *"Trace the order validation logic."*
1.  **Semantic Search:** Spring AI searches the Vector Store for `OrderValidator` concepts.
2.  **Graph Lookahead:**
    * The Agent queries the `JGraphT` graph:
    * *"Incoming Edges to OrderValidator?"* $\to$ Returns `OrderController`.
    * *"Outgoing Edges from OrderValidator?"* $\to$ Returns `UserRepository`.
3.  **Context Assembly:** The Agent retrieves the source code for the *Controller*, the *Validator*, and the *Repository Interface*.
4.  **Synthesis:** The LLM explains the flow using the multi-file context.

---

## 6. Data Flow & Logic Example

**Scenario:** User asks *"How does the `/login` endpoint work?"*

1.  **Search:** System searches for `@PostMapping("/login")` or similar terms.
2.  **Hit:** Finds `AuthController.login()`.
3.  **Graph Traversal:**
    * `AuthController` injects `AuthService` (Interface).
    * **Resolution:** Graph sees `AuthServiceImpl` implements `AuthService`.
    * `AuthServiceImpl.login()` calls `PasswordEncoder.matches()`.
4.  **Prompt Generation:**
    > "Context includes: `AuthController.java`, `AuthServiceImpl.java`, `SecurityConfig.java`. Explain the data flow from the HTTP request to the password check."
5.  **Output:** A step-by-step explanation referencing the specific classes and methods.

## 7. Success Metrics
* **Resolution Accuracy:** >90% success rate in linking Interfaces to Implementations without full classpath.
* **Latency:** Full RAG response within <10 seconds on standard M-series/local hardware.
* **Privacy:** Zero data egress.
* **Resilience:** System must gracefully handle non-compilable code (e.g., syntax errors in one file shouldn't stop the whole indexing).

## 8. Architectural Decisions & Trade-offs (ADRg)
* **ADR-001: In-Memory Graph vs. ROI**
    * *Decision:* Use `JGraphT` (In-Memory).
    * *Trade-off:* Limited by heap size, but provides microsecond traversal speeds critical for deciding *what* to fetch in real-time. For MVPs targeting projects < 500k LOC, this is acceptable.
* **ADR-002: Source-Only Analysis**
    * *Decision:* Use `JavaParser` on source files, do not require compiled `.class` files.
    * *Trade-off:* Type resolution will be imperfect (cannot resolve dependencies in external JARs unless we index those too). We accept "Best Effort resolution" to lower the barrier to entry (no need to build the project to analyze it).
* **ADR-003: Local LLM**
    * *Decision:* Ollama managed externally or via Testcontainers.
    * *Trade-off:* Higher system requirements (RAM), but guarantees privacy and zero-cost operation.

## 8. Future Scope (Post-MVP)
* **Visual Generation:** Output Mermaid.js sequence diagrams for the traced flows.
* **Test Generation:** Automatically generate JUnit tests for identified flows.