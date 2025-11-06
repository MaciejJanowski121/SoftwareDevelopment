describe('Reservation', () => {
    beforeEach(() => {
        cy.clearCookies();
        cy.clearLocalStorage();

        // --- Authentifizierung: simuliere eingeloggt Benutzer ---
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: {
                username: 'max',
                role: 'ROLE_USER',
                fullName: 'Max Mustermann',
                email: 'max@example.com',
            },
        }).as('authCheck');
    });

    it('erstellt erfolgreich eine Reservierung und navigiert zu /reservations/my', () => {
        // --- Stub für verfügbare Tische ---
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: [
                { id: 1, tableNumber: 5, numberOfSeats: 4 },
                { id: 2, tableNumber: 6, numberOfSeats: 2 },
            ],
        }).as('available');

        // --- Erfolgreiche Reservierungsanfrage ---
        cy.intercept('POST', '**/api/reservations', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: {
                id: 100,
                tableNumber: 5,
                startTime: '2025-11-07T18:00:00',
                endTime: '2025-11-07T20:00:00',
            },
        }).as('reservationOk');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // --- Setze eine gültige Startzeit (morgen 18:00 Uhr) ---
        const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
        tomorrow.setHours(18, 0, 0, 0);
        const pad = (n) => (n < 10 ? '0' + n : n);
        const futureLocal = `${tomorrow.getFullYear()}-${pad(
            tomorrow.getMonth() + 1
        )}-${pad(tomorrow.getDate())}T${pad(tomorrow.getHours())}:${pad(
            tomorrow.getMinutes()
        )}`;

        cy.get('#startTime').invoke('attr', 'min', '1900-01-01T00:00');
        cy.get('#startTime').clear().type(futureLocal);
        cy.get('#duration').select('120');
        cy.get('#tableNumber').select('5');
        cy.findByRole('button', { name: /reservieren/i }).click();

        // --- Backend bestätigt Reservierung ---
        cy.wait('@reservationOk');
        cy.location('pathname').should('eq', '/reservations/my');
    });

    it('zeigt eine verständliche Meldung, wenn ein Tisch bereits reserviert ist (409)', () => {
        // --- Verfügbare Tische ---
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: [{ id: 1, tableNumber: 1, numberOfSeats: 4 }],
        }).as('available');

        // --- Konflikt: Backend meldet, dass der Tisch inzwischen reserviert ist ---
        cy.intercept('POST', '**/api/reservations', {
            statusCode: 409,
            headers: { 'content-type': 'text/plain' },
            body: 'Tisch ist bereits reserviert.',
        }).as('conflict');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // --- Setze eine Startzeit in naher Zukunft ---
        const now = new Date(Date.now() + 2 * 60 * 60 * 1000);
        now.setSeconds(0, 0);
        const pad = (n) => (n < 10 ? '0' + n : n);
        const startLocal = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(
            now.getDate()
        )}T${pad(now.getHours())}:${pad(now.getMinutes())}`;

        cy.get('#startTime').invoke('attr', 'min', '1900-01-01T00:00');
        cy.get('#startTime').clear().type(startLocal);
        cy.get('#duration').select('120');
        cy.get('#tableNumber').select('1');
        cy.findByRole('button', { name: /reservieren/i }).click();

        // --- Backend-Antwort 409 prüfen ---
        cy.wait('@conflict');
        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .and('contain', 'reserviert');
    });

    it('validiert Frontend-Regeln: Vergangenheit und Überschreitung der 22:00 Uhr Grenze', () => {
        // --- Keine echten Daten von Backend laden ---
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: [],
        }).as('availSilenced');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // --- 1) Reservierung in der Vergangenheit ---
        const pastLocal = (() => {
            const d = new Date(Date.now() - 60 * 60 * 1000); // eine Stunde zurück
            d.setSeconds(0, 0);
            const pad = (n) => (n < 10 ? '0' + n : n);
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(
                d.getDate()
            )}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
        })();

        cy.get('#startTime').invoke('attr', 'min', '1900-01-01T00:00');
        cy.get('#startTime').clear().type(pastLocal);
        cy.get('#duration').select('120');
        cy.findByRole('button', { name: /reservieren/i }).click();

        // --- Erwartete Fehlermeldung für Vergangenheit ---
        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .invoke('text')
            .then((t) => {
                const txt = t.toLowerCase();
                expect(/vergangen|nicht möglich/.test(txt)).to.equal(true);
            });

        // --- 2) Startzeit 21:30 + 120 Minuten (endet nach 22:00) ---
        const lateLocal = (() => {
            const now = new Date();
            const s = new Date();
            s.setSeconds(0, 0);
            s.setHours(21, 30, 0, 0);
            if (s <= now) s.setDate(s.getDate() + 1);
            const pad = (n) => (n < 10 ? '0' + n : n);
            return `${s.getFullYear()}-${pad(s.getMonth() + 1)}-${pad(
                s.getDate()
            )}T${pad(s.getHours())}:${pad(s.getMinutes())}`;
        })();

        cy.get('#startTime').invoke('attr', 'min', '1900-01-01T00:00');
        cy.get('#startTime').clear().type(lateLocal);

        // --- Prüfe, dass 120 Minuten deaktiviert ist ---
        cy.get('#duration').find('option[value="120"]').should('be.disabled');

        // --- Absenden und Fehlermeldung prüfen ---
        cy.findByRole('button', { name: /reservieren/i }).click();

        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .invoke('text')
            .then((t) => {
                const txt = t.toLowerCase();
                expect(/22:00|spätestens|zulässig/.test(txt)).to.equal(true);
            });

        cy.location('pathname').should('eq', '/reservations/new');
    });
});