
# 🎙️ Smart Meeting Agent

> **Plateforme intelligente de gestion de réunions : Transcription temps réel et génération de rapports automatisés via IA.**

---

## 🌟 Aperçu du Projet
Le **Smart Meeting Agent** est une solution complète conçue pour automatiser la documentation des réunions. Il permet de créer des espaces de communication en direct, de transcrire les échanges et de produire un rapport structuré immédiatement après la session grâce à l'intelligence artificielle.

### 🚀 Fonctionnalités Clés
* **Real-time Communication** : Salons audio/vidéo fluides via une instance **LiveKit locale**.
* **Transcription Live** : Transformation de la voix en texte durant l'échange.
* **AI Report Generation** : Analyse automatique ultra-rapide via l'**API Groq** (Llama 3 / Mixtral) pour extraire les points clés et les actions à entreprendre.
* **Dashboard de Gestion** : Historique des réunions et stockage des rapports générés notifiée en temps réel grace à SSE.

---

## 🛠️ Architecture du Projet
Le projet est divisé en trois modules principaux pour une séparation nette des responsabilités :

1.  **`/frontend` (Angular)** : Interface utilisateur moderne pour la gestion des réunions et l'affichage des transcriptions.
2.  **`/backend` (Spring Boot)** : Orchestration du système, gestion de la base de données et sécurité, notifée le dashboard en intégrant SSE.
3.  **`/sworked-ia` (Python)** : Le "cerveau" du projet gérant le flux audio LiveKit et l'inférence LLM avec Groq.

---

## 🏗️ Pipeline Technique
1.  **Flux Audio** : Capturé par le client et géré par le serveur **LiveKit (Self-hosted)**.
2.  **Traitement** : Le module Python récupère les flux pour la reconnaissance vocale.
3.  **Intelligence Artificielle** : Envoi des transcriptions à l'**API Groq** pour une génération de résumé en quelques millisecondes.
4.  **Persistance** : Spring Boot centralise et sauvegarde le rapport final.

---

## ⚙️ Installation & Configuration

### ⚠️ Important : Variables d'environnement
Ce projet utilise des fichiers `.env` pour la sécurité. Il ne contient pour le clé réel remplacer le par votre vraie clé.

#### 1. Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run

```

#### 2. Frontend (Angular)

```bash
cd frontend
npm install
ng serve

```

#### 3. AI Worker (Python)

```bash
cd worker-ia
pip install -r requirements.txt
python agent.py start







