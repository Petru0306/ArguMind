# Context Proiect: ArguMind

## 1. Identitate & Scop
* **Nume:** ArguMind
* **Context:** Proiect dezvoltat pentru Olimpiada Națională de Inovare Digitală - Infoeducație (Secțiunea Web / Software Educațional).
* **Concept:** Platformă web gamificată de dezbateri turn-based (PRO vs. CONTRA), unde arbitrajul (punctaj, feedback retoric, detectare erori logice) este realizat de un **AI Judge**.

---

## 2. Stack Tehnic & Arhitectură
* **Backend:** Java 17 cu **Spring Boot 3.4.x** (Maven)
* **Bază de date:** **SQL (PostgreSQL)** cu Spring Data JPA / Hibernate
* **Securitate:** Spring Security + BCrypt + XSS Validation (Jsoup)
* **Real-time:** Spring WebSockets (STOMP)
* **Integrare AI:** OpenAI API / Gemini API via Custom REST Client

---

## 3. Criterii InfoEducație (Strategie Scor Maxim)
Proiectul este aliniat la baremul oficial pentru Secțiunea Web (100 puncte):

*   **Inginerie Web (25p):** Arhitectură stratificată, algoritm ELO propriu, matchmaking optimizat, interogări SQL eficiente (indecși).
*   **Funcționalitate (20p):** Integrare AI complexă, Modul de Administrare (Admin Dashboard), Offline storage (localStorage).
*   **Design & UX (20p):** Design propriu (fără template-uri), Accesibilitate (Text-to-Speech), Internaționalizare (i18n - RO/EN).
*   **Originalitate (15p):** Concept de "Esport Intelectual", arbitraj AI pe erori logice (Ad Hominem, Strawman).
*   **Securitate (10p):** Protecție XSS (Jsoup), SQL Injection (JPA), BCrypt hashing, CSRF protection.
*   **Prezentare (10p):** Documentație tehnică detaliată, studii de caz.

---

## 4. Structura Proiectului (Organizare)
*   **`model/`**: Entități JPA (User, Match, Argument, LogicalFallacy).
*   **`dto/`**: Obiecte de transfer pentru decuplare API.
*   **`repository/`**: Interfețe Spring Data JPA.
*   **`service/`**: Logica de business (Game Engine, AI Judge, ELO Algorithm).
*   **`controller/`**: Endpoint-uri REST și WebSocket Message Mapping.
*   **`config/`**: Configurații (Security, WebSocket, i18n).

---

## 5. Fluxul Principal de Joc (The Core Loop)
1.  **Inițiere:** Matchmaking pe temă + Mod de joc (BLITZ, RAPID, STANDARD).
2.  **Runde (Turn-based):** 2-3 runde cu timere sincronizate.
3.  **Evaluare AI:** Procesare asincronă, detectare erori logice.
4.  **Verdict:** Update ELO, feedback detaliat, salvare în localStorage.

---

## 6. Personalități AI Judge
*   **Socrates:** Strict pe definiții și logică pură.
*   **Cicero:** Accent pe stil, vocabular și retorică.
*   **Gordon:** Sarcastic, feedback tăios.

---

## 7. Instrucțiuni pentru Gemini (System Prompt Local)
1. **Fără cod redundant:** Soluții curate, Spring Boot 3.4.
2. **Arhitectură DTO:** Separare totală Entitate vs API.
3. **Programare Defensivă:** Validare input (XSS), tratare erori, logare.
4. **Originalitate:** Implementare algoritmi proprii (ELO) acolo unde baremul cere contribuție proprie.

---

## 8. Status Curent & Roadmap
- [x] Generare proiect & Configurare Git.
- [x] Configurare PostgreSQL & Entități de bază.
- [x] **Etapa 1: Game State Engine** (Matchmaking, Rounds).
- [x] **Etapa 2: Spring Security & BCrypt**.
- [x] **Etapa 3: Integrare AI Judge (Mocked logic & DB Sync)**.
- [x] **Etapa 4: Real-time via WebSockets (Core implementation)**.
- [x] **Etapa 5: Rafinare Securitate WebSockets & Validare XSS (Jsoup)**.
- [x] **Etapa 6: Algoritm ELO Propriu & Moduri de Joc (Blitz/Standard)**.
- [ ] **Etapa 7: Modul de Administrare & i18n (Suport limbi)**.
- [ ] **Etapa 8: Frontend Brut (Tailwind) + LocalStorage + Text-to-Speech**.
- [ ] **Etapa 9: Documentație & Optimizări Performanță**.
