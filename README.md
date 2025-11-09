Restaurant-Reservierungssystem

Fullstack Web Application – Spring Boot & React

Diese Anwendung ermöglicht es Benutzer:innen, Tischreservierungen in einem Restaurant digital zu verwalten.
Das System besteht aus einem Spring Boot Backend (REST API) und einem React Frontend (Single Page Application).
Ziel des Projekts ist eine sichere, responsive und moderne Lösung zur Verwaltung von Reservierungen mit rollenbasierter Zugriffskontrolle.


restaurant-reservierungssystem/
├── backend/                   # Spring Boot REST API (Java 17)
│   ├── controller/            # REST-Endpunkte für Authentifizierung & Reservierungen
│   ├── service/               # Geschäftslogik, z. B. UserService, ReservationService
│   ├── model/                 # Entitäten, z. B. User, Reservation
│   ├── dto/                   # Data Transfer Objects, z. B. RegisterRequest, ReservationDTO
│   ├── jwtservices/           # JWT-Erzeugung & Validierung
│   ├── configuration/         # Sicherheits- und CORS-Konfiguration
│   ├── exception/             # Einheitliches Fehler- und Exception-Handling
│   └── repository/            # JPA-Repositories für Datenbankzugriff
│
├── frontend/                  # React Single-Page Application (React 18.3.1)
│   ├── src/components/        # UI-Komponenten, z. B. ReservationForm, Validation, LogoutButton
│   ├── src/pages/             # Seiten, z. B. Login, Register, MyAccount, AdminPanel
│   ├── src/styles/            # CSS-Dateien & Layouts
│   ├── src/tests/             # Komponententests (React Testing Library)
│   └── cypress/e2e/           # End-to-End-Tests (z. B. Registrierung & Reservierung)
│
├── README.md                  # Projektdokumentation & Setup-Anleitung
├── pom.xml                    # Maven-Konfiguration für das Backend
├── package.json               # npm-Konfiguration für das Frontend
└── .gitignore

Installation & Start

Backend :
cd backend
mvn clean install
mvn spring-boot:run

Der Server startet standardmäßig auf:
http://localhost:8080

Frontend:
cd frontend
npm install
npm start

Die React-App läuft unter:
http://localhost:3000

Hauptfunktionen
•	Benutzer:
•	Registrierung & Login (JWT-basiert)
•	Eigene Reservierungen erstellen, anzeigen und löschen
•	Administrator:
•	Übersicht aller Reservierungen
•	Löschen oder Bearbeiten von Buchungen
•	Systemfunktionen:
•	Validierung von Formularen (Frontend & Backend)
•	Role-based Access Control (ROLE_USER / ROLE_ADMIN)
•	API-Kommunikation über Fetch mit credentials: include
•	Responsives UI (CSS-basierte Layouts)

Teststrategie
•   Backend-Tests:
•	Unit- & Integrationstests :
•	JUnit 5, Mockito, Spring Boot Test
•	Frontend-Tests:
•	@testing-library/react für Komponenten
•	Cypress für End-to-End-Szenarien

Start der Tests Backend :
# im Ordner backend/
mvn test

Frontend-Tests (React Testing Library) :
# im Ordner frontend/
npm test -- --watchAll=false

End-to-End-Tests (Cypress)
# im Ordner frontend/
npm run cy:open  
npx cypress run