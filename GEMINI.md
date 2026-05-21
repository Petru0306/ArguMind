# Context Proiect: ArguMind

## 1. Identitate & Scop
* **Nume:** ArguMind
* **Context:** Proiect dezvoltat pentru Olimpiada Națională de Inovare Digitală - Infoeducație (Secțiunea Web / Software Educațional).
* **Concept:** Platformă web gamificată de dezbateri turn-based (PRO vs. CONTRA), unde arbitrajul (punctaj, feedback retoric, detectare erori logice) este realizat de un **AI Judge**.

---

## 2. Stack Tehnic & Arhitectură
* **Backend:** Java 17 cu **Spring Boot 3.4.x** (Maven)
* **Bază de date:** **SQL (PostgreSQL)** cu Spring Data JPA / Hibernate
* **Securitate:** Spring Security (urmează a fi implementat)
* **Real-time:** Spring WebSockets (urmează a fi implementat)
* **Integrare AI:** OpenAI API / Spring AI (urmează a fi implementat)

---

## 3. Structura Proiectului (Organizare)
Proiectul respectă o arhitectură stratificată (Layered Architecture) pentru a asigura separarea responsabilităților:

*   **`model/`**: Entități JPA care mapează tabelele din baza de date (User, Match, Argument).
*   **`dto/`**: Data Transfer Objects pentru comunicarea cu exteriorul. Decuplează API-ul de baza de date.
*   **`repository/`**: Interfețe Spring Data JPA pentru operațiuni CRUD.
*   **`service/`**: Logica de business a aplicației.
*   **`controller/`**: Endpoint-uri REST care expun funcționalitățile către frontend.

---

## 4. Fluxul Principal de Joc (The Core Loop)
1.  **Inițiere:** Matchmaking pe o temă -> Rol PRO sau CONTRA.
2.  **Runde (Turn-based):** 2-3 runde. PRO scrie (2-3 min), apoi CONTRA răspunde.
3.  **Evaluare AI:** Procesare în fundal, detectare erori logice.
4.  **Verdict:** Fișă de scor, feedback retoric, actualizare ELO.

---

## 5. Structura Aplicației (Pagini)
1.  **Landing Page:** Minimalist academic, statistici live, CTA Login/Register.
2.  **Dashboard:** Profil (ELO, Rang), Grafice evoluție, Matchmaking central.
3.  **Lobby:** Selecție teme pe categorii, Arene Private.
4.  **Arena (Real-time):** Split-screen PRO/CONTRA, Timere, Status "Oponentul scrie...".
5.  **Post-Match:** Fișă scor (Logică, Claritate, Dovezi, Retorică), Highlight erori logice cu tooltips.
6.  **Leaderboard:** Clasament global sortat după ELO.

---

## 6. Personalități AI Judge
*   **Socrates:** Strict pe definiții și logică pură.
*   **Cicero:** Accent pe stil, vocabular și retorică.
*   **Gordon:** Sarcastic, feedback tăios (gaming style).

---

## 7. Schema Bazei de Date (Implementată)

### Tabela `users`
* `id` (UUID / BIGINT, PK)
* `username` (VARCHAR, Unique)
* `password` (VARCHAR, Encoded)
* `elo_rating` (INT, Default: 1000)
* `rank_title` (VARCHAR, Default: 'NOVICE')

### Tabela `matches`
* `id` (UUID / BIGINT, PK)
* `topic` (VARCHAR)
* `status` (VARCHAR: PENDING, ACTIVE, FINISHED, PROCESSING_AI)
* `pro_user_id` (FK -> users.id)
* `contra_user_id` (FK -> users.id)
* `winner_id` (FK -> users.id, Nullable)

### Tabela `arguments`
* `id` (BIGINT, PK)
* `match_id` (FK -> matches.id)
* `user_id` (FK -> users.id)
* `round_number` (INT)
* `text_content` (TEXT)
* `created_at` (TIMESTAMP)

---

## 8. Instrucțiuni pentru Gemini (System Prompt Local)
Când asiști în acest proiect, respectă următoarele reguli:
1. **Fără cod redundant:** Oferă soluții curate, axate pe Spring Boot 3 și baze de date relaționale. Folosește Lombok pentru a reduce codul boilerplate.
2. **Arhitectură DTO:** Nu expune niciodată entitățile direct în Controller. Folosește DTO-uri pentru request și response.
3. **Abordare incrementală:** Construim proiectul pas cu pas (MVP). Nu sări direct la funcționalități avansate (WebSockets/AI) până nu avem nucleul CRUD funcțional.
4. **Stil:** Răspunsuri prompte, axate direct pe implementare și configurare CLI/Terminal.

---

## 9. Status Curent & Roadmap
- [x] Generare proiect Spring Initializr.
- [x] Configurare Repository Git.
- [x] Configurare PostgreSQL (Bază de date: `argumind`, User: `ArguMind`).
- [x] Implementare Arhitectură Stratificată (`model`, `dto`, `repository`, `service`, `controller`).
- [x] Implementare Endpoint Înregistrare (`/api/auth/register`).
- [x] **Etapa 1: Game State Engine (Backend)**
    - [x] Matchmaking logic (Pending -> Active).
    - [x] Round control (Submit argument, check turns).
    - [x] State: `PROCESSING_AI` transition.
- [x] **Etapa 2: Configurare Spring Security & BCrypt**.
- [ ] **Etapa 3: Integrare LLM (Gemini/OpenAI API)**.
- [ ] **Etapa 4: Real-time via WebSockets**.
- [ ] **Etapa 5: Frontend (Landing, Dashboard, Arena)**.
- [ ] **Etapa 6: Gamification & Achievements**.
