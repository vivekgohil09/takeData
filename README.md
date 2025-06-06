# 🧠 Offline LLM-Powered Data Description Generator

This project uses an **offline large language model (LLM)** to process user prompts, retrieve relevant data from a **relational database**, and generate human-readable summaries or descriptions — all without requiring internet access.

---

## 🚀 Features

- ✨ Query-based content generation using LLM
- 🗃️ Fetches structured data from a local database (e.g., MySQL/PostgreSQL)
- 💬 Accepts user prompt (e.g., "Explain sales data for May")
- 🧠 Uses an offline LLM (like LLaMA, GGUF-based models, or GPT4All)
- 🧾 Outputs a human-readable description or summary

---

## 🏗️ Tech Stack

- **Java Spring Boot** – Backend API
- **Offline LLM** – via Python (e.g., `llama-cpp`, `GPT4All`, `Ollama`, etc.)
- **MySQL/PostgreSQL** – Data storage
- **Python** – For invoking the model (via REST or subprocess)
- **JPA/Hibernate** – For database interaction

---

## 🛠️ How It Works

1. 🔍 User sends a prompt like:  
   `"Give a summary of orders for client X in March"`

2. 📦 Spring Boot fetches relevant data from the database using query logic.

3. 🧠 The backend sends structured data to the **offline LLM**.

4. 📝 The LLM generates a natural language description.

5. 📤 The final summary is sent as a response to the client.

---

## 📦 Example Prompt → Response

**Prompt:**
> "Summarize stock holdings of Fund ABC for April"

**Response:**
> "Fund ABC held 15 stocks in April 2024, with the highest allocation (12.5%) in Infosys. The top sectors included IT and Pharma, indicating a tech-heavy portfolio."

---
