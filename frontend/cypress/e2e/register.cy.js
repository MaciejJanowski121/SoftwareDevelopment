

/**
 * End-to-End-Test für den Registrierungsprozess.
 *
 * <p>Dieser Cypress-Test prüft sowohl den erfolgreichen Ablauf der Benutzerregistrierung
 * als auch den Fehlerszenario bei bereits vorhandener E-Mail-Adresse.</p>
 *
 * <ul>
 *   <li>Mockt den initialen <code>GET /auth/auth_check</code> mit 401 (nicht eingeloggt).</li>
 *   <li>Simuliert den Aufruf von <code>POST /auth/register</code> mit validen Nutzerdaten.</li>
 *   <li>Überprüft, dass der Benutzer nach erfolgreicher Registrierung automatisch zu
 *       <code>/myaccount</code> weitergeleitet wird.</li>
 *   <li>Testet zusätzlich den Konfliktfall (HTTP 409), bei dem eine bereits existierende
 *       E-Mail-Adresse eine sichtbare Fehlermeldung im Formular auslöst.</li>
 *   <li>Verwendet <code>cy.intercept</code> für API-Mocking, um deterministische und isolierte Tests zu ermöglichen.</li>
 * </ul>
 *
 * @test
 * @framework Cypress
 * @returns {void} Führt UI-Tests für Erfolg und Fehlerfall der Benutzerregistrierung aus.
 */
describe('Registrierung', () => {
    beforeEach(() => {
        cy.clearCookies();
        cy.clearLocalStorage();
    });

    it('registriert erfolgreich und navigiert zu /myaccount', () => {
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 401,
            headers: { 'content-type': 'application/json' },
            body: 'Unauthorized'
        }).as('auth401');

        cy.visit('http://localhost:3000/register');
        cy.wait('@auth401');

        cy.intercept('POST', '**/auth/register', (req) => {
            const body = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;

            expect(body).to.include.all.keys('fullName', 'email', 'phone', 'password');
            expect(body.fullName).to.equal('Max Mustermann');
            expect(body.email).to.equal('max@example.com');

            req.reply({
                statusCode: 200,
                headers: { 'content-type': 'application/json' },
                body: {
                    username: 'max',
                    role: 'ROLE_USER',
                    fullName: 'Max Mustermann',
                    email: 'max@example.com',
                    phone: '+49 170 1234567',
                },
            });
        }).as('register');

        cy.get('#fullName').should('exist').type('Max Mustermann');
        cy.get('#email').type('max@example.com');
        cy.get('#phone').type('+491701234567');
        cy.get('#password').type('secret123');

        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 200,
            headers: { 'content-type': 'application/json' },
            body: {
                username: 'max',
                role: 'ROLE_USER',
                fullName: 'Max Mustermann',
                email: 'max@example.com',
                phone: '+49 170 1234567'
            }
        }).as('auth200');

        cy.findByRole('button', { name: /registrieren/i }).click();

        cy.wait('@register');
        cy.wait('@auth200');
        cy.location('pathname').should('eq', '/myaccount');
    });

    it('zeigt einen Fehler, wenn E-Mail bereits vergeben ist (409)', () => {
        cy.intercept('GET', '**/auth/auth_check', { statusCode: 401 }).as('auth401');

        cy.visit('http://localhost:3000/register');
        cy.wait('@auth401');

        cy.intercept('POST', '**/auth/register', {
            statusCode: 409,
            headers: { 'content-type': 'text/plain' },
            body: 'E-Mail bereits vergeben'
        }).as('registerConflict');

        cy.get('#fullName').type('Max Mustermann');
        cy.get('#email').type('max@example.com');
        cy.get('#phone').type('+491701234567');
        cy.get('#password').type('secret123');

        cy.findByRole('button', { name: /registrieren/i }).click();

        cy.wait('@registerConflict');
        cy.findByRole('alert', { timeout: 6000 })
            .should('be.visible')
            .invoke('text')
            .then((t) => {
                const text = t.toLowerCase().trim();
                expect(
                    /bereits|existiert|konflikt|conflict|fehl|fehler|registrierung/.test(text) ||
                    text.length > 0
                ).to.be.true;
            });

        cy.location('pathname').should('eq', '/register');
    });
});