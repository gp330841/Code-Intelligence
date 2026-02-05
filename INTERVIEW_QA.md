# Interview Q&A Practice - Code Intelligence Project

**Role**: Senior Backend Engineer / System Architect
**Format**: 10 Questions (Progressive Difficulty)

---

## Level 1: Basics (Spring Boot & Usage)

### Q1. Why does your `application.yml` define separate configurations for `chroma` and `ollama`?
**Ideal Answer**:
"We follow the **Separation of Concerns** principle. Chroma is our database layer (stateful), while Ollama is our computation layer (stateless inference). Configuring them independently allows us to scale them differently—for example, moving Chroma to a managed cloud cluster while keeping Ollama local for privacy, or swapping Ollama for OpenAI without touching the database config."
**Weak Answer**:
"Because they are different tools so they need different keys." (Misses the architectural coupling/decoupling argument).

---

## Level 2: Data Structures & Algorithms

### Q2. You mentioned "AST Traversal". Which Tree Traversal algorithm does `JavaParser` use, and why does that matter for performance?
**Ideal Answer**:
"It essentially uses a **Depth-First Search (DFS)** via the Visitor Pattern. This matters because stack depth can become an issue with deeply nested code (like anonymous inner classes inside lambdas). However, since code nesting rarely exceeds typically 50-100 levels, DFS is memory efficient compared to BFS for this specific structure."
**Weak Answer**:
"It just visits every node." (Fails to identify DFS or the Visitor pattern).

---

## Level 3: RAG & Vector Search

### Q3. Explain "Cosine Similarity" to a Junior Developer. Why not just use Euclidean Distance?
**Ideal Answer**:
"Imagine two arrows pointing from `(0,0)`. Euclidean distance measures how far the *tips* of the arrows are. Cosine similarity measures the *angle* between them. In text embeddings, the *length* of the vector often represents the length of the document, which we don't care about—we care about the *direction* (the topic). So we use Cosine to check if they point to the same topic, regardless of length."
**Weak Answer**:
"Cosine is just better for text." (No mathematical intuition).

---

## Level 4: Docker & Networking

### Q4. Your frontend container listens on port 80 (inside) but 3000 (outside). The backend is on 8080. If I change the backend port to 9090 on the host, do I need to update the Frontend code?
**Ideal Answer**:
"It depends on *how* the frontend talks to the backend. If the React app runs in the **browser**, it accesses the API via the *Host's* public URL. So yes, if I change the host-bound port from `8080:8080` to `9090:8080`, I must update the API_URL in the frontend code. Docker internal ports don't matter to the browser."
**Weak Answer**:
"No, because Docker networking handles it." (Confuses server-side container communication with client-side browser communication).

---

## Level 5: System Failure Analysis

### Q5. What happens if the user uploads a 5GB ZIP file? How does your system fail?
**Ideal Answer**:
"First, Nginx (if configured) or Spring Boot's Tomcat connector will likely reject it due to `max-swallow-size` or `multipart.max-file-size` (default is often 10MB or configured to 500MB). If it passes that, we risk an **OutOfMemoryError (OOM)** during JavaParser execution because we might try to load the entire AST into heap. We should implement streaming unzip and single-file parsing to handle this."
**Weak Answer**:
"It will just take a long time." (Ignores memory limits and hard configuration caps).

---

## Level 6: Distributed Systems

### Q6. We need to deploy this to AWS for 1000 users. The "In-Memory" vector DB is crashing. What is your migration plan?
**Ideal Answer**:
"1. **Decouple**: Switch from local Docker Chroma to a managed **ChromaDB Cluster** or **AWS OpenSearch** (with k-NN).
2.  **State Management**: Stop mounting local volumes. Use S3 to store raw file artifacts.
3.  **Consistency**: Introduction of an ingestion queue (SQS/Kafka) so that uploads don't block the web server thread. The 'Ingestion Worker' pulls from SQS and writes to the remote Vector DB."
**Weak Answer**:
"Just enable Auto-scaling on the EC2 instance." (Scaling the compute doesn't fix the stateful DB bottleneck).

---

## Level 7: Consistency & Concurrency

### Q7. Two users upload the same project "Project X" at the exact same time. What happens to the Vector DB?
**Ideal Answer**:
"Without optimistic locking or unique constraints, we get a **Race Condition**. Both threads parse files and insert vectors. We end up with duplicate vectors (double the results for every search). We need to implement a 'lock' on the Project Name or a 'cleanup before insert' transaction: `DELETE FROM vectors WHERE project_id='X'; INSERT ...`"
**Weak Answer**:
"It's fine, the database handles it." (Chroma is often eventually consistent or simplistic; logic must be in the app).

---

## Level 8: Advanced RAG

### Q8. The user asks: "How are the `User` and `Order` classes related?". Simple RAG fails because the keywords "User" and "Order" appear in 100 files. How do you solve this?
**Ideal Answer**:
"Vector search fails here because it finds *mention* of User, not the *relationship*. We need **Graph-RAG**.
1. Use our dependency graph (JGraphT) to find the edge `User -> (hasMany) -> Order`.
2. Retrieve the code for *User* AND the code for *Order* based on this graph connection, not just vector similarity.
3. Feed both class definitions into the LLM context."
**Weak Answer**:
"Just increase the `k` retrieval count to 50." (Pollutes context with noise).

---

## Level 9: Security

### Q9. Your app accepts a file path to ingest. I send `/etc/passwd`. What happens?
**Ideal Answer**:
"If the backend code uses `new File(path)` without validation, this is a **Path Traversal / Local File Inclusion (LFI)** vulnerability. The app encounters a `PermissionDenied` (hopefully) or actually reads the file and embeds it into the vector layer, allowing me to ask 'What are the system users?' and get the `/etc/passwd` content. We must sanitize inputs to allow only specific user-workspace directories."
**Weak Answer**:
"Java throws an error." (Doesn't explain the security implication).

---

## Level 10: System Evolution

### Q10. We want to support Python and C++ ingestion next week. How much code do you need to rewrite?
**Ideal Answer**:
"The `JavaParser` logic is tightly coupled. We need to implement the **Strategy Pattern** for the Extraction Layer.
- Create an interface `CodeParser { parse(file): AST }`.
- Rename `JavaParserService` to `JavaStrategy`.
- Implement `PythonStrategy` (using Python's `ast` module or a Java-based Python parser).
- The Embedding/RAG layer remains *unchanged* (it just takes text segments), so ~70% of the code is reusable."
**Weak Answer**:
"We just need to change the file extension filter." (Ignores that JavaParser cannot parse C++).
