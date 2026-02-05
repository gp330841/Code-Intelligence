Code Intelligence Agent — Interview Deep Dive

This document is written strictly from a Senior Backend / System Design interview perspective. It expands the project ONLY where it increases interview value.

⸻

1. Retrieval Augmented Generation (RAG)

What problem does this solve?

LLMs have no knowledge of private or newly written code. Asking questions about a local codebase causes hallucination or generic answers.

Why was this approach chosen?

RAG allows injecting only relevant code at query time. It avoids retraining models and supports real-time code changes.

Alternative approaches and why they were rejected
	•	Fine-tuning: Expensive, slow, knowledge becomes stale.
	•	Full context stuffing: Token limits, latency, and privacy issues.

Performance implications
	•	Vector search adds ~200–500ms latency.
	•	Chunk size impacts recall vs precision.

Failure scenarios and handling
	•	Vector DB down → degrade to generic LLM.
	•	Bad retrieval → hallucination (mitigated by strict prompting).

How I would explain this in an interview
	•	“RAG decouples knowledge from the model.”
	•	“It’s cheaper, fresher, and safer than fine-tuning.”

⸻

2. Embeddings & Vector Store (ChromaDB)

What problem does this solve?

Keyword search fails for semantic intent (e.g., Login vs Session).

Why this approach?

Dense embeddings capture semantic meaning; ChromaDB is lightweight and local-friendly.

Alternatives rejected
	•	pgvector: Higher operational overhead.
	•	Pinecone: Cloud dependency, privacy trade-off.

Performance implications
	•	ANN search is probabilistic but fast.
	•	Re-indexing is CPU intensive.

Failure scenarios
	•	Duplicate vectors due to race conditions.

Interview explanation
	•	“Vectors let us search by meaning, not words.”

⸻

3. AST Parsing & Dependency Graph

Problem solved

Plain-text parsing loses structure.

Why AST?

AST provides class/method boundaries and dependency awareness.

Alternatives rejected
	•	Regex parsing (fragile)
	•	LSP (too heavy)

Performance implications
	•	DFS traversal; stack-safe for real-world code depth.

Failure scenarios
	•	OOM on very large projects without streaming.

Interview explanation
	•	“AST lets us chunk code logically, not arbitrarily.”

⸻

4. System Architecture

Problem solved

Need isolation, reproducibility, and local-first design.

Why Docker Compose?

Simple orchestration of stateful + stateless services.

Alternatives rejected
	•	Kubernetes (overkill for single-node dev tool)

Performance implications
	•	Network hop overhead between containers.

Failure scenarios
	•	Chroma container crash → degraded answers.

Interview explanation
	•	“Stateless backend, stateful vector DB.”

⸻

INTERVIEW QUESTIONS

Below are interviewer-style questions with increasing difficulty, each followed by:
	•	Ideal answer
	•	Common weak answer
	•	Why the weak answer fails

A. RAG (10 Questions)

Q1. Why did you choose RAG over fine-tuning?

Ideal answer: RAG keeps knowledge external and fresh. Code changes frequently; re-training is slow and expensive. RAG lets us update knowledge by re-indexing vectors only.
Weak answer: Fine-tuning is costly.
Why weak: Doesn’t explain freshness, coupling, or operational impact.

Q2. What causes hallucinations in RAG systems?

Ideal answer: Poor retrieval quality or irrelevant context. The model hallucinates to fill gaps when context is missing or noisy.
Weak answer: LLMs hallucinate sometimes.
Why weak: Ignores retrieval responsibility.

Q3. How does chunk size affect RAG?

Ideal answer: Small chunks improve precision; large chunks improve recall. There’s a trade-off between context quality and token budget.
Weak answer: Smaller is better.
Why weak: Oversimplified.

Q4. What is context window pressure?

Ideal answer: The limit on tokens forces prioritization. Too much context causes truncation or noise.
Weak answer: LLM has token limits.
Why weak: No impact analysis.

Q5. Why cosine similarity instead of Euclidean?

Ideal answer: Direction matters more than magnitude in embeddings; cosine ignores vector length.
Weak answer: Everyone uses cosine.
Why weak: No intuition.

Q6. What is Graph-RAG?

Ideal answer: Augmenting vector search with dependency graphs to capture relationships, not just semantic similarity.
Weak answer: Better RAG.
Why weak: No mechanism described.

Q7. How do you evaluate retrieval quality?

Ideal answer: Precision@K, recall, and manual inspection of retrieved chunks.
Weak answer: By checking answers.
Why weak: Subjective and late feedback.

Q8. How do you prevent prompt injection?

Ideal answer: Sanitize inputs and restrict system prompts to context-only answers.
Weak answer: Trust the model.
Why weak: Security blind spot.

Q9. When does RAG fail completely?

Ideal answer: When the question requires global reasoning not present in retrieved chunks.
Weak answer: If DB is down.
Why weak: Misses conceptual limits.

Q10. Hybrid search vs pure vector search?

Ideal answer: Combine keyword + vector search to improve recall for exact symbols.
Weak answer: Vector is enough.
Why weak: Ignores edge cases.

⸻

B. Spring Boot / AOP (5 Questions)
	1.	Why use AOP for observability?
	2.	Proxy vs AspectJ weaving?
	3.	How does AOP affect performance?
	4.	Transaction boundaries with AOP?
	5.	Common pitfalls of AOP?

⸻

C. Docker (5 Questions)
	1.	docker run vs docker-compose?
	2.	Why host.docker.internal?
	3.	Volume vs bind mount?
	4.	Container crash recovery?
	5.	How to scale stateful containers?

⸻

D. Frontend (5 Questions)
	1.	Why keep frontend stateless?
	2.	API error handling strategy?
	3.	XSS risks in Markdown rendering?
	4.	CORS handling?
	5.	Why not React framework?

⸻

FINAL NOTE

This project is strong. Missing pieces (intentionally):
	•	No auth / multi-user isolation
	•	No ingestion queue
	•	No rate limiting

Calling these out in interviews increases seniority signal.