// cypress/e2e/reservation.cy.js
describe('Reservation', () => {
    const pad = (n) => (n < 10 ? '0' + n : String(n));
    const stepMinute = (m) => (parseInt(m, 10) >= 30 ? '30' : '00');
    const splitLocal = (local) => {
        const [datePart, timePart] = local.split('T');
        const [h, m] = timePart.split(':');
        return { datePart, h: String(Number(h)), m };
    };

    beforeEach(() => {
        cy.clearCookies();
        cy.clearLocalStorage();

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
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            body: [
                { id: 1, tableNumber: 5, numberOfSeats: 4 },
                { id: 2, tableNumber: 6, numberOfSeats: 2 },
            ],
        }).as('available');

        cy.intercept('POST', '**/api/reservations*', {
            statusCode: 200,
            body: {
                id: 100,
                tableNumber: 5,
                startTime: '2025-11-07T18:00:00',
                endTime: '2025-11-07T20:00:00',
            },
        }).as('reservationOk');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // jutro 18:00 (zawsze przyszłość)
        const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
        tomorrow.setHours(18, 0, 0, 0);
        const futureLocal = `${tomorrow.getFullYear()}-${pad(
            tomorrow.getMonth() + 1
        )}-${pad(tomorrow.getDate())}T${pad(tomorrow.getHours())}:${pad(
            tomorrow.getMinutes()
        )}`;
        const { datePart, h, m } = splitLocal(futureLocal);

        cy.get('#startDate').clear().type(datePart);
        cy.get('#hour').select(h);
        cy.get('#minute').select(stepMinute(m));

        cy.wait('@available');
        cy.get('#duration').select('120');
        cy.get('#tableNumber').select('5');

        cy.findByRole('button', { name: /reservieren/i }).click();

        cy.wait('@reservationOk');
        cy.location('pathname').should('eq', '/reservations/my');
    });

    it('zeigt eine verständliche Meldung, wenn ein Tisch bereits reserviert ist (409)', () => {
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            body: [{ id: 1, tableNumber: 1, numberOfSeats: 4 }],
        }).as('available');

        cy.intercept('POST', '**/api/reservations*', {
            statusCode: 409,
            headers: { 'content-type': 'text/plain; charset=utf-8' }, // <— DODANE
            body: 'Tisch ist bereits reserviert.',
        }).as('conflict');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // jutro 18:00 (zawsze dozwolone 120 min i na pewno przyszłość)
        const t = new Date(Date.now() + 24 * 60 * 60 * 1000);
        t.setHours(18, 0, 0, 0);
        const local = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(
            t.getDate()
        )}T${pad(t.getHours())}:${pad(t.getMinutes())}`;
        const { datePart, h, m } = splitLocal(local);

        cy.get('#startDate').clear().type(datePart);
        cy.get('#hour').select(h);
        cy.get('#minute').select(stepMinute(m));

        cy.wait('@available');               // <— najpierw poczekaj aż lista się załaduje
        cy.get('#duration').select('120');
        cy.get('#tableNumber').select('1');  // <— dopiero po available
        cy.findByRole('button', { name: /reservieren/i }).click();

        cy.wait('@conflict');
        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .and('contain', 'reserviert');
    });

    it('validiert Frontend-Regeln: Vergangenheit und Überschreitung der 22:00 Uhr Grenze', () => {
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            body: [],
        }).as('availSilenced');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // (1) Wczoraj 18:00 → zawsze przeszłość
        const y = new Date(Date.now() - 24 * 60 * 60 * 1000);
        y.setHours(18, 0, 0, 0);
        const pastLocal = `${y.getFullYear()}-${pad(y.getMonth() + 1)}-${pad(
            y.getDate()
        )}T${pad(y.getHours())}:${pad(y.getMinutes())}`;
        const p = splitLocal(pastLocal);

        cy.get('#startDate').clear().type(p.datePart);
        cy.get('#hour').select(p.h);
        cy.get('#minute').select(stepMinute(p.m));
        cy.get('#duration').select('120');
        cy.findByRole('button', { name: /reservieren/i }).click();

        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .invoke('text')
            .then((t) => {
                const txt = t.toLowerCase();
                expect(/vergangen|nicht möglich/.test(txt)).to.equal(true);
            });

        // (2) 21:30 (jutro, jeśli dziś za późno) → 120 min powinno być zablokowane
        const s = new Date();
        s.setSeconds(0, 0);
        s.setHours(21, 30, 0, 0);
        if (s <= new Date()) s.setDate(s.getDate() + 1);
        const lateLocal = `${s.getFullYear()}-${pad(s.getMonth() + 1)}-${pad(
            s.getDate()
        )}T${pad(s.getHours())}:${pad(s.getMinutes())}`;
        const L = splitLocal(lateLocal);

        cy.get('#startDate').clear().type(L.datePart);
        cy.get('#hour').select(L.h);
        cy.get('#minute').select(stepMinute(L.m));

        cy.get('#duration').find('option[value="120"]').should('be.disabled');
    });
});