Fișă Tehnică de Proiect: ArguMind
Competiție: Olimpiada Națională de Inovare Digitală - Infoeducație
Categorie propusă: Web
1. Descrierea Generală a Proiectului
ArguMind este o platformă digitală interactivă de tip web application concepută pentru a
transforma dezbaterile clasice și exersarea retoricii într-o experiență gamificată, supervizată de
un arbitru bazat pe Inteligență Artificială. Proiectul își propune să rezolve o problemă acută în
societatea digitală actuală: degradarea calității discursului public și lipsa abilităților de gândire
critică în rândul tinerilor.
Sistemul permite utilizatorilor să se înregistreze, să aleagă teme de dezbatere stringente (de
exemplu: „Uniformele școlare ar trebui să fie obligatorii?”, „AI-ul ajută sau distruge educația?”,
„Temele pentru acasă mai sunt utile?”) și să intre în dueluri directe, structurate pe runde.
Componenta revoluționară ("Partea Wow") constă în înlocuirea subiectivității umane cu un AI
Judge capabil să analizeze logic, semantic și comportamental argumentele aduse.
2. Funcționalități Cheie
●​ Creare și gestionare dueluri (Debate Matchmaking): Utilizatorii pot iniția meciuri
publice sau private, alegând dintr-o listă predefinită de teme sau propunând teme noi.
●​ Arbitraj Automatizat (AI Judge): Un modul avansat analizează fiecare replică pe baza a
patru piloni fundamentali: rigoare logică, claritate conceptuală, calitatea dovezilor
prezentate și fair-play (respectarea adversarului, absența atacurilor la persoană).
●​ Detectarea erorilor logice (Fallacy Detection): Algoritmul identifică activ abateri de la
logica formală, cum ar fi Strawman, Ad Hominem, False Dilemma sau Slippery Slope,
penalizându-le în timp real.
●​ Sistem de Punctaj și Clasament (Leaderboard): Performanțele sunt cuantificate prin
puncte de experiență (XP) și rating Elo, generând o ierarhie competitivă ce stimulează
îmbunătățirea continuă.
●​ Generator de Contraargumente și Mod Singleplayer: Utilizatorii se pot antrena singuri
împotriva unui oponent controlat de AI, configurat pe diverse stiluri de argumentare
(socratic, agresiv, academic).
●​ Feedback Retoric Personalizat: La finalul fiecărui duel, utilizatorii primesc un raport
detaliat cu sugestii de îmbunătățire a structurii discursului și vocabularului utilizat.3. Arhitectura Tehnică și Tehnologii Utilizate
Aplicația este structurată pe o arhitectură robustă de tip Client-Server, utilizând tehnologii
consacrate în mediul enterprise pentru a asigura scalabilitate, securitate și performanță optimă.
ComponentăTehnologie selectatăRol și Justificare Tehnică
Backend FrameworkJava (Spring Boot)Asigură nucleul logic al
aplicației. Spring Boot
permite gestionarea
eficientă a rutelor REST API,
securitatea prin Spring
Security și managementul
sigur al dependințelor,
oferind timp de răspuns
rapid și stabilitate ridicată.
Bază de dateSQL (PostgreSQL / MySQL)Sistem relațional utilizat
pentru stocarea persistentă
a conturilor de utilizatori,
istoricului meciurilor,
structurii tematicilor,
log-urilor de arbitraj și
clasamentului global.
Garantează integritatea
datelor prin constrângeri
ACID.
Persistență / ORMHibernate / JPAFacilitează maparea
obiectual-relațională între
clasele Java și tabelele
bazei de date SQL,
reducând volumul de cod
boilerplate și optimizând
interogările complexe prinComponentă
Tehnologie selectată
Rol și Justificare Tehnică
mecanisme de caching.
Integrare AIOpenAI API / Spring AIPermite conectarea sigură și
asincronă la modele de
limbaj de mari dimensiuni
(LLM) pentru analiza
textului, extragerea erorilor
logice prin tehnici de Prompt
Engineering și generarea
structurată a feedback-ului
în format JSON.
Interfață UtilizatorThymeleaf + Bootstrap /
ReactAsigură un frontend dinamic,
complet responsive, adaptat
atât pentru ecrane desktop,
cât și pentru dispozitive
mobile, optimizând
încărcarea resurselor și
experiența de utilizare.
4. Argumente pentru Juriul Infoeducație
1.​ Impact Educațional Major: Stimulează gândirea critică, combate dezinformarea și îi
învață pe elevi cum să structureze un discurs argumentat corect, eliminând agresivitatea
verbală din mediul online.
2.​ Complexitate Tehnică Solidă: Combinarea unui backend robust în Java cu baze de date
relaționale și procesare de limbaj natural (NLP) demonstrează stăpânirea unor concepte
software avansate și de actualitate pe piața muncii.
3.​ Originalitate și Gamificare: Conceptul de „Arenă” digitală îmbină utilul cu plăcutul,
transformând o activitate considerată de mulți plictisitoare (studiul logicii și retoricii) într-un
joc captivant și competitiv.
