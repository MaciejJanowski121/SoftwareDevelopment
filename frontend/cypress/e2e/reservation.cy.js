
/**
 * End-to-End-Test für den Reservierungsvorgang.
 *
 * <p>Dieser Test überprüft das vollständige Verhalten des Reservierungsformulars
 * – von der erfolgreichen Erstellung über Fehlermeldungen bis hin zu
 * UI-Validierungen für Zeit- und Datumsgrenzen.</p>
 *
 * <ul>
 *   <li>Mockt <code>/auth/auth_check</code>, um einen eingeloggenen Benutzer zu simulieren.</li>
 *   <li>Validiert, dass eine Reservierung erfolgreich angelegt und zur Seite
 *       <code>/reservations/my</code> weitergeleitet wird.</li>
 *   <li>Testet den Konfliktfall (<code>HTTP 409</code>) mit einer aussagekräftigen
 *       Fehlermeldung bei doppelter Tischreservierung.</li>
 *   <li>Überprüft UI-Regeln: keine Reservierung in der Vergangenheit und
 *       maximale Endzeit 22:00 Uhr.</li>
 *   <li>Ignoriert temporäre React-Fehler („document is null“) während des schnellen Rerenderings.</li>
 * </ul>
 *
 * @test
 * @framework Cypress
 * @returns {void} Führt E2E-Tests für das Reservierungsformular aus.
 */

describe('Reservation', () => {
    const pad = (n) => (n < 10 ? '0' + n : String(n));
    const stepMinute = (m) => (parseInt(m, 10) >= 30 ? '30' : '00');
    const splitLocal = (local) => {
        const [datePart, timePart] = local.split('T');
        const [h, m] = timePart.split(':');
        return { datePart, h: String(Number(h)), m };
    };

    beforeEach(() => {

        cy.on('uncaught:exception', (err) => {
            if (/document/i.test(err?.message || '')) return false;
        });

        cy.clearCookies();
        cy.clearLocalStorage();


        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: { username: 'max', role: 'ROLE_USER', fullName: 'Max Mustermann', email: 'max@example.com' },
        }).as('authCheck');
    });

    it('erstellt erfolgreich eine Reservierung und navigiert zu /reservations/my', () => {
        cy.intercept(
            { method: 'GET', url: '**/api/reservations/available*' },
            {
                statusCode: 200,
                body: [
                    { id: 1, tableNumber: 5, numberOfSeats: 4 },
                    { id: 2, tableNumber: 6, numberOfSeats: 2 },
                ],
            }
        ).as('available');

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


        const t = new Date(Date.now() + 24 * 60 * 60 * 1000);
        t.setHours(18, 0, 0, 0);
        const futureLocal = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}T${pad(t.getHours())}:${pad(
            t.getMinutes()
        )}`;
        const { datePart, h, m } = splitLocal(futureLocal);

        cy.get('#startDate', { timeout: 10000 }).should('exist').clear().type(datePart);
        cy.get('#hour').should('exist').select(h, { force: true });
        cy.get('#minute').should('exist').select(stepMinute(m), { force: true });

        cy.wait('@available');
        cy.get('#duration').select('120', { force: true });
        cy.get('#tableNumber').select('5', { force: true });

        cy.findByRole('button', { name: /reservieren/i }).click();

        cy.wait('@reservationOk');
        cy.location('pathname').should('eq', '/reservations/my');
    });

    it('zeigt eine verständliche Meldung, wenn ein Tisch bereits reserviert ist (409)', () => {
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            body: [{ id: 1, tableNumber: 1, numberOfSeats: 4 }],
        }).as('available');

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

        const t = new Date(Date.now() + 24 * 60 * 60 * 1000);
        t.setHours(18, 0, 0, 0);
        const local = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}T${pad(t.getHours())}:${pad(
            t.getMinutes()
        )}`;
        const { datePart, h, m } = splitLocal(local);

        cy.get('#startDate').clear().type(datePart);
        cy.get('#hour').select(h, { force: true });
        cy.get('#minute').select(stepMinute(m), { force: true });

        cy.wait('@available');
        cy.get('#duration').select('120', { force: true });
        cy.get('#tableNumber').select('1', { force: true });

        cy.findByRole('button', { name: /reservieren/i }).click();
        cy.wait('@conflict');

        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .and('contain', 'reserviert');
    });

    it('validiert UI: keine Reservierungen in der Vergangenheit und keine über 22:00 Uhr hinaus', () => {
        cy.intercept('GET', '**/api/reservations/available*', {
            statusCode: 200,
            body: [{ id: 99, tableNumber: 3, numberOfSeats: 2 }],
        }).as('avail');

        cy.visit('http://localhost:3000/reservations/new');
        cy.wait('@authCheck');


        const y = new Date(Date.now() - 24 * 60 * 60 * 1000);
        y.setHours(18, 0, 0, 0);
        const pastLocal = `${y.getFullYear()}-${pad(y.getMonth() + 1)}-${pad(y.getDate())}T${pad(y.getHours())}:${pad(
            y.getMinutes()
        )}`;
        const p = splitLocal(pastLocal);

        cy.get('#startDate').clear().type(p.datePart);
        cy.get('#hour').select(p.h, { force: true });
        cy.get('#minute').select(stepMinute(p.m), { force: true });

        cy.wait('@avail');
        cy.get('#duration').select('120', { force: true });
        cy.get('#tableNumber').select('3', { force: true });

        cy.findByRole('button', { name: /reservieren/i }).click();

        cy.get('[data-cy="form-error"]', { timeout: 8000 })
            .should('be.visible')
            .invoke('text')
            .then((txt) => {
                const t = txt.toLowerCase();
                expect(/vergangen|nicht möglich/.test(t)).to.eq(true);
            });


        const s = new Date();
        s.setSeconds(0, 0);
        s.setHours(21, 30, 0, 0);
        if (s <= new Date()) s.setDate(s.getDate() + 1);
        const lateLocal = `${s.getFullYear()}-${pad(s.getMonth() + 1)}-${pad(s.getDate())}T${pad(s.getHours())}:${pad(
            s.getMinutes()
        )}`;
        const L = splitLocal(lateLocal);

        cy.get('#startDate').clear().type(L.datePart);
        cy.get('#hour').select(L.h, { force: true });
        cy.get('#minute').select(stepMinute(L.m), { force: true });

        cy.get('#duration').find('option[value="120"]').should('be.disabled');
    });
});