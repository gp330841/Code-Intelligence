# Code Intelligence Agent

A powerful RAG-based Code Intelligence Agent built with Java, Spring Boot, and Spring AI. It ingests your local project, understands its structure (AST + Dependency Graph), and allows you to chat with your codebase using a local LLM (Ollama).

## Features

- **Code Ingestion**: Upload local project folders directly via the UI.
- **Dependency Awareness**: Builds a graph of code units (Classes, Methods) to understand relationships.
- **Semantic Search**: Uses Vector Embeddings (ChromaDB) to find relevant code.
- **RAG (Retrieval Augmented Generation)**: Generates context-aware answers using `deepseek-r1`.
- **Observability**: Real-time System Logs, Performance Metrics (AOP), and Database Inspection.
- **Modern UI**: Dark mode interface with Markdown rendering and syntax highlighting.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.3, Spring AI
- **Vector Store**: ChromaDB
- **LLM**: Ollama (DeepSeek R1 / CodeLlama)
- **Frontend**: HTML5, Vanilla JS, Highlight.js, Marked.js
- **Containerization**: Docker, Docker Compose


## System Architecture

> **Note**: For a deep dive into System Design, Trade-offs, and failure scenarios please see [SENIOR_ENGINEER_INTERVIEW_GUIDE.md](SENIOR_ENGINEER_INTERVIEW_GUIDE.md).

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

## Prerequisites

1.  **Docker Desktop**: Installed and running.
2.  **Ollama**: Installed locally.
    - Pull the embedding model: `ollama pull nomic-embed-text`
    - Pull the chat model: `ollama pull deepseek-r1` (or update `application.yml` to your preferred model)

## How to Run

1.  **Start the Application**:
    ```bash
    docker-compose up --build --force-recreate
    ```
2.  **Access UI**: Open `http://localhost:3000` in your browser.

## How to Use

1.  **Load Project**:
    - Click **Choose Files** in the header.
    - Select your project directory.
    - Click **Load Project**. The agent will ingest and vectorize the code.

2.  **Chat**:
    - Type your questions in the chat bar (e.g., "How does the ingestion pipeline work?").
    - The agent will answer with context from your code.

3.  **Inspect DB**:
    - Click **Inspect DB** to view the raw vectors and metadata stored in ChromaDB.

4.  **System Logs**:
    - Click **System Logs** to view real-time backend logs.

## Configuration

- **Models**: Configure `src/main/resources/application.yml`:
    ```yaml
    spring:
      ai:
        ollama:
          chat:
            model: deepseek-r1
          embedding:
            model: nomic-embed-text
    ```
- **File Upload Limits**: Default set to 500MB (configurable in `application.yml`).

---
Built with ❤️ by Code Intelligence Team.






