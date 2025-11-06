// cypress/e2e/admin.cy.js

describe('Admin Panel Zugriff', () => {
    beforeEach(() => {
        cy.clearCookies();
        cy.clearLocalStorage();
    });

    it('redirectet ROLE_USER vom /admin auf /myaccount', () => {
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: {
                username: 'maciej',
                role: 'ROLE_USER',
                fullName: 'Maciej',
                email: 'maciej@example.com',
            },
        }).as('authUser');

        cy.visit('http://localhost:3000/admin');
        cy.wait('@authUser');
        cy.location('pathname').should('eq', '/myaccount');
    });

    it('zeigt Admin UI für ROLE_ADMIN', () => {
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: {
                username: 'admin',
                role: 'ROLE_ADMIN',
                fullName: 'Administrator',
                email: 'admin@example.com',
            },
        }).as('authAdmin');

        // (opcjonalnie) jeżeli panel jednak pobiera listę — zostawiamy stub,
        // ale NIE czekamy na niego (apka może go nie wywoływać na wejściu)
        cy.intercept('GET', '**/api/reservations/all', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: [],
        }).as('reservationsAll');

        cy.visit('http://localhost:3000/admin');
        cy.wait('@authAdmin');

        // 1) URL faktycznie na /admin
        cy.location('pathname').should('eq', '/admin');

        // 2) UI admina jest widoczne — wybierz JEDEN z poniższych selektorów,
        // w zależności co masz w widoku admina:

        // a) jeśli masz nagłówek:
        cy.findByRole('heading', { name: /admin|verwaltung|reservierungen/i })
            .should('be.visible');

        // b) jeśli masz wrapper z data-cy:
        // cy.get('[data-cy="admin-panel"]').should('be.visible');

        // c) jeśli masz przycisk/akcję admina:
        // cy.findByRole('button', { name: /neue tisch|export|refresh|laden/i }).should('be.visible');

        // NIE czekamy na @reservationsAll, bo request może nie wystąpić
    });
});