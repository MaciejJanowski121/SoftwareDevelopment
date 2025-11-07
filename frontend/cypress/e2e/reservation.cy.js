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

        // zawsze zalogowany użytkownik
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: { username: 'max', role: 'ROLE_USER', fullName: 'Max Mustermann', email: 'max@example.com' },
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

        cy.intercept('POST', '**/api/reservations', {
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

        // jutro 18:00
        const t = new Date(Date.now() + 24 * 60 * 60 * 1000);
        t.setHours(18, 0, 0, 0);
        const futureLocal = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}T${pad(t.getHours())}:${pad(
            t.getMinutes()
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
        // dostępny stół 1 – żeby button nie był disabled
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            body: [{ id: 1, tableNumber: 1, numberOfSeats: 4 }],
        }).as('available');

        // ⬇️ 409 jako ProblemDetail JSON (tak jak oczekuje frontend)
        cy.intercept('POST', '**/api/reservations', {
            statusCode: 409,
            headers: { 'content-type': 'application/problem+json' },
            body: {
                type: 'https://docs.example/errors/table-already-reserved',
                title: 'Conflict',
                status: 409,
                detail: 'Dieser Tisch ist im gewählten Zeitraum bereits reserviert.',
                instance: '/api/reservations',
            },
        }).as('conflict');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // jutro 18:00
        const t = new Date(Date.now() + 24 * 60 * 60 * 1000);
        t.setHours(18, 0, 0, 0);
        const local = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}T${pad(t.getHours())}:${pad(
            t.getMinutes()
        )}`;
        const { datePart, h, m } = splitLocal(local);

        cy.get('#startDate').clear().type(datePart);
        cy.get('#hour').select(h);
        cy.get('#minute').select(stepMinute(m));

        cy.wait('@available');
        cy.get('#duration').select('120');
        cy.get('#tableNumber').select('1');

        cy.findByRole('button', { name: /reservieren/i }).click();
        cy.wait('@conflict');

        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .and('contain', 'reserviert'); // pochodzi z mapowania type → przyjazny tekst
    });

    it('waliduje UI: przeszłość i granicę 22:00', () => {
        // zwracamy jakikolwiek stół, żeby można było kliknąć submit
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            body: [{ id: 99, tableNumber: 3, numberOfSeats: 2 }],
        }).as('avail');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');

        // (1) wczoraj 18:00 → przeszłość
        const y = new Date(Date.now() - 24 * 60 * 60 * 1000);
        y.setHours(18, 0, 0, 0);
        const pastLocal = `${y.getFullYear()}-${pad(y.getMonth() + 1)}-${pad(y.getDate())}T${pad(y.getHours())}:${pad(
            y.getMinutes()
        )}`;
        const p = splitLocal(pastLocal);

        cy.get('#startDate').clear().type(p.datePart);
        cy.get('#hour').select(p.h);
        cy.get('#minute').select(stepMinute(p.m));
        cy.wait('@avail'); // duration change może odpalić /available
        cy.get('#duration').select('120');
        cy.get('#tableNumber').select('3'); // w nowym formularzu submit wymaga stołu

        cy.findByRole('button', { name: /reservieren/i }).click();

        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .invoke('text')
            .then((txt) => {
                const t = txt.toLowerCase();
                expect(/vergangen|nicht möglich/.test(t)).to.eq(true);
            });

        // (2) 21:30 → 120 min powinno być zablokowane (max do 22:00)
        const s = new Date();
        s.setSeconds(0, 0);
        s.setHours(21, 30, 0, 0);
        if (s <= new Date()) s.setDate(s.getDate() + 1);
        const lateLocal = `${s.getFullYear()}-${pad(s.getMonth() + 1)}-${pad(s.getDate())}T${pad(s.getHours())}:${pad(
            s.getMinutes()
        )}`;
        const L = splitLocal(lateLocal);

        cy.get('#startDate').clear().type(L.datePart);
        cy.get('#hour').select(L.h);
        cy.get('#minute').select(stepMinute(L.m));
        cy.get('#duration').find('option[value="120"]').should('be.disabled');
    });
});