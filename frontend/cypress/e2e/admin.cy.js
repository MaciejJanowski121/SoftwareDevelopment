
/**
 * End-to-End-Test für den Adminbereich.
 *
 * <p>Dieser Cypress-Test überprüft die Zugriffskontrolle auf die Admin-Seite
 * anhand der Benutzerrolle, die vom Endpunkt <code>/auth/auth_check</code> geliefert wird.</p>
 *
 * <ul>
 *   <li><strong>Rolle ROLE_USER:</strong> Der Zugriff auf <code>/admin</code> führt
 *       automatisch zu einer Weiterleitung auf <code>/myaccount</code>.</li>
 *   <li><strong>Rolle ROLE_ADMIN:</strong> Der Zugriff bleibt erlaubt, und die
 *       Admin-Oberfläche wird korrekt angezeigt (Überschrift, Buttons oder Container sichtbar).</li>
 *   <li>Mockt alle Netzwerkanfragen (<code>cy.intercept</code>), um das Backend zu simulieren
 *       und deterministische Testergebnisse zu gewährleisten.</li>
 * </ul>
 *
 * @test
 * @framework Cypress
 * @returns {void} Führt zwei vollständige Browsertests zur Rollenvalidierung aus.
 */
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

        cy.intercept('GET', '**/api/reservations/all', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: [],
        }).as('reservationsAll');

        cy.visit('http://localhost:3000/admin');
        cy.wait('@authAdmin');

        cy.location('pathname').should('eq', '/admin');

        cy.findByRole('heading', { name: /admin|verwaltung|reservierungen/i })
            .should('be.visible');
    });
});