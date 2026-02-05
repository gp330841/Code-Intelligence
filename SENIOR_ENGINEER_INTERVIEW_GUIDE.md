# Senior Engineer Interview Guide - Code Intelligence Agent

This document contains the deep-dive system design analysis and the comprehensive interview question bank (25 questions). It serves as your primary resource for "Senior Backend / System Architect" interviews.

---

# Part 1: System Design & Architecture

**Perspective**: Senior Engineering logic, focusing on problem statements, trade-offs, and failure scenarios.

## 1. RAG (Retrieval Augmented Generation)

### Problem Statement
Large Language Models (LLMs) have two major limitations:
1.  **Knowledge Cutoff**: They don't know about events or code written after their training date.
2.  **Private Context**: They have zero visibility into your local, private codebase.
Fine-tuning a model on your code is expensive, slow, and requires re-training for every code change.

### Why this Approach?
We chose **RAG** (Retrieval Augmented Generation) with a local LLM (`Ollama`).
-   **Dynamic Context**: By injecting relevant code snippets into the prompt at runtime, the LLM "learns" your code on-the-fly without training.
-   **Data Privacy**: Everything runs locally. Your proprietary code never leaves your machine (unlike GPT-4 API).

### Alternatives Considered
-   **Fine-Tuning CodeLlama**: Rejected because it requires GPU resources and doesn't handle real-time code updates matching the edit-compile-debug cycle.
-   **Context Stuffing (Gemini 1.5 Pro)**: Putting the *entire* codebase in the context window. Rejected due to latency (processing 1M tokens takes time) and cost/privacy concerns with cloud APIs.

### Trade-offs
-   **Context Fragmentation**: We only retrieve the top-k chunks. If the answer requires global understanding (e.g., "How does data flow through the entire app?"), RAG might miss the connection between distant modules.
-   **Latency**: The retrieval step (Vector Search + Disk I/O) adds ~200-500ms before the LLM even starts generating.

### Failure Scenarios
-   **Retrieval Fail**: If ChromaDB is down, the system degrades to a generic chat bot.
-   **Hallucination**: If the retrieved chunks are irrelevant (poor similarity score), the LLM might confidently invent a wrong answer. Mitigation: Prompt engineering ("Answer ONLY from context").

## 2. Embeddings & Vector Search

### Problem Statement
Keyword search (`grep` or ElasticSearch) fails on semantic queries. Searching for "Auth" might miss `LoginController` or `SessionManager`. We need to search by *intent*.

### Why this Approach?
We use **ChromaDB** with `nomic-embed-text`.
-   **Nomic Embeddings**: Specifically trained for long-context text and code, offering a 768-dimensional dense vector space.
-   **ChromaDB**: Lightweight, open-source vector store, runs easily in Docker.

### Alternatives Considered
-   **PostgreSQL (pgvector)**: Adds schema management overhead compared to Chroma's schema-less nature for prototyping.
-   **Pinecone**: Rejected for 100% offline/local requirement.

### Trade-offs
-   **Approximate Nearest Neighbor (ANN)**: Probabilistic search using HNSW indexes. Fast, but might miss the exact best match.
-   **Re-indexing Cost**: Validating embeddings requires re-running the model on changed files (CPU intensive).

## 3. Code Extraction (AST Analysis)

### Problem Statement
Treating code files as plain text blobs destroys structural meaning. We need to distinguish between `class`, `method`, and `import`.

### Why this Approach?
**JavaParser (AST) + JGraphT**.
-   **AST Traversal**: Chunk code by logical units (Methods) rather than lines.
-   **Dependency Graph**: Directed graph of class dependencies enables "Graph-RAG" (retrieving dependencies of a match).

### Alternatives Considered
-   **Regex Parsing**: Fragile, fails on nested structures.
-   **LSP**: Too complex to integrate as a standalone library.

## 4. System Architecture

### Overview
Microservices architecture using Docker Compose.

```mermaid
graph TD
    User[User / Browser] -->|HTTP| Frontend[Frontend (React + Nginx)]
    Frontend -->|API Requests| Backend[Backend (Spring Boot)]
    Backend -->|gRPC| Chroma[ChromaDB (Vector Store)]
    Backend -->|HTTP| Ollama[Ollama (Inference)]
    Backend -->|Read| FS[File System]
```

### Network Decisions
-   **Internal Overlay Network**: Backend <-> Chroma (Private).
-   **Host Loopback**: Backend <-> Ollama (`host.docker.internal`) to leverage host GPU.

### Scalability Path
1.  **Queue-Based Ingestion**: Decouple upload from processing using RabbitMQ/SQS.
2.  **Distributed Vector DB**: Migrate Chroma to a managed cluster.
3.  **Load Balancing**: Scale stateless Spring Boot backend.

---

# Part 2: Comprehensive Interview Q&A (25 Questions)

## Section 1: RAG & AI (10 Questions)

### Q1. What is the "Lost in the Middle" phenomenon in RAG?
**Ideal Answer**: "LLMs pay more attention to the start and end of the context. If critical info is in the middle retrieved chunk, it might be ignored. We mitigate this by **Re-ranking** results to place high-relevance chunks at the edges, or keeping `top-k` small."

### Q2. How does `nomic-embed-text` work?
**Ideal Answer**: "It uses a Transformer encoder to map code semantics into a **768-dimensional dense vector space**. Similar contexts (`login`, `session`) map to nearby points in this manifold, capturing intent beyond keywords."

### Q3. Why ChromaDB instead of a Map?
**Ideal Answer**: "A Map is O(N) search. ChromaDB uses **HNSW graphs** for Approximate Nearest Neighbor (ANN) search in O(log N), essential for latency with high vector counts."

### Q4. How to handle "Global" questions (e.g. "Summarize architecture")?
**Ideal Answer**: "RAG fails here. We need **Global Summarization** (iterative summarization of file batches) or a **Knowledge Graph** to query high-level design nodes."

### Q5. Chunking Strategy: Code vs Text?
**Ideal Answer**: "Text splits by paragraph. Code MUST split by **Logical Unit (AST)**. Breaking a method or loop in half destroys syntax and semantic meaning."

### Q6. Temperature setting for RAG?
**Ideal Answer**: "**Low (0.1-0.3)**. We want factual, deterministic answers based on context, not creative hallucination."

### Q7. What is Hybrid Search?
**Ideal Answer**: "Merging **Sparse Vectors** (Keyword/BM25) with **Dense Vectors** (Embeddings) using Reciprocal Rank Fusion (RRF). Captures both exact variable names and semantic concepts."

### Q8. Why Ollama on Host vs Container?
**Ideal Answer**: "GPU pass-through to Docker is complex (drivers, toolkit). Running on Host (`host.docker.internal`) lets Ollama natively use Metal/CUDA with zero config."

### Q9. Using Dependency Graphs?
**Ideal Answer**: "If Vector Search finds `Class A`, checked the Graph. If `A extends B`, we must also retrieve `B` to give the LLM the full context (Connected Subgraph)."

### Q10. Measuring RAG Quality?
**Ideal Answer**: "Use **RAGAS** framework. Measure **Context Precision** (relevance), **Context Recall** (completeness), and **Faithfulness** (answer matches context)."

## Section 2: Spring Boot & Backend (5 Questions)

### Q11. AOP for Metrics vs Logs?
**Ideal Answer**: "AOP separates **Cross-Cutting Concerns**. Adding logs to every method violates DRY. `@Around` advice lets us maintain timing logic in one place."

### Q12. Spring Bean Lifecycle?
**Ideal Answer**: "Instantiate -> Populate Properties -> Aware Interfaces -> PostProcessBefore -> @PostConstruct -> PostProcessAfter -> Ready -> @PreDestroy."

### Q13. Processing 5GB Uploads?
**Ideal Answer**: "**Asynchronous Processing**. Return 202 Accepted. Stream the file (no processing in RAM). Use a Queue/Worker pattern to process in background."

### Q14. @Component vs @Service vs @Repository?
**Ideal Answer**: "All are `@Component`. `@Service` is semantic. `@Repository` adds DataAccessException translation. Useful for AOP targeting."

### Q15. How Auto-Configuration works?
**Ideal Answer**: "Scans classpath. `@ConditionalOnClass` triggers bean creation (e.g., `OllamaChatModel`) if standard libraries are found, unless overridden by user config."

## Section 3: Docker & DevOps (5 Questions)

### Q16. `depends_on` functionality?
**Ideal Answer**: "Controls **start order**, NOT readiness. It won't wait for the DB port to open. Real readiness requires healthchecks or `wait-for-it` scripts."

### Q17. Multi-stage Builds?
**Ideal Answer**: "Stage 1 (Build) has Node/NPM. Stage 2 (Run) has only Nginx + static files. Reduces image size from 500MB to 5MB."

### Q18. Data Persistence?
**Ideal Answer**: "Containers are ephemeral. We use **Volumes** to map host folders to container paths, ensuring data survives restarts."

### Q19. EXPOSE vs Ports?
**Ideal Answer**: "`EXPOSE` is documentation. `ports` in Compose actually **publishes** (maps) the port to the host."

### Q20. Debugging crashing containers?
**Ideal Answer**: "`docker logs` for output. `docker inspect` for exit codes. Override entrypoint to `/bin/sh` to inspect filesystem."

## Section 4: Frontend (React) (5 Questions)

### Q21. `useEffect` timing?
**Ideal Answer**: "Runs **after** render commit. Dependency array controls frequency (empty = mount only)."

### Q22. Solving Prop Drilling?
**Ideal Answer**: "**Context API** for global state, or State Management libraries (Redux/Zustand), or Component Composition."

### Q23. Virtual DOM?
**Ideal Answer**: "Lightweight in-memory DOM representation. React diffs it against previous version to minimize expensive real DOM updates. Improves dev productivity."

### Q24. Displaying 10k logs?
**Ideal Answer**: "**Virtualization** (React Window). Render only visible items (e.g. 20 rows) to keep DOM light."

### Q25. Handling CORS?
**Ideal Answer**: "Dev: Proxy pass in `package.json`. Prod: Nginx reverse proxy serving both on same domain."
