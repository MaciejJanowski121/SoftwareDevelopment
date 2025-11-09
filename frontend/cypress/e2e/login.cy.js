

/**
 * End-to-End-Test für den Login-Prozess der Anwendung.
 *
 * <p>Dieser Cypress-Test simuliert den vollständigen Anmeldeablauf
 * mit E-Mail und Passwort, inklusive Authentifizierungsprüfung und Weiterleitung.</p>
 *
 * <ul>
 *   <li>Mockt den initialen <code>GET /auth/auth_check</code> mit 401 (nicht eingeloggt).</li>
 *   <li>Validiert den Request für <code>POST /auth/login</code> – prüft Header und Body-Felder.</li>
 *   <li>Antwortet mit einem simulierten erfolgreichen Login (<code>ROLE_USER</code>).</li>
 *   <li>Mockt den Folge-Request <code>GET /auth/auth_check</code> mit 200 (eingeloggt).</li>
 *   <li>Stellt sicher, dass der Benutzer nach erfolgreichem Login zu <code>/myaccount</code> weitergeleitet wird.</li>
 * </ul>
 *
 * @test
 * @framework Cypress
 * @returns {void} Führt einen vollständigen UI-Login-Test inklusive API-Intercepts und Navigation aus.
 */
describe('Login (E-Mail + Passwort)', () => {
    beforeEach(() => {
        cy.clearCookies();
        cy.clearLocalStorage();
    });

    it('loggt korrekt ein und navigiert zu /myaccount', () => {
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 401,
            body: 'Invalid Token',
        }).as('authCheck');

        cy.intercept('POST', '**/auth/login', (req) => {
            expect(req.headers['content-type']).to.include('application/json');
            expect(req.body).to.have.keys(['email', 'password']);
            expect(req.body.email).to.equal('maciej@example.com');

            req.reply({
                statusCode: 200,
                body: {
                    username: 'maciej',
                    role: 'ROLE_USER',
                    fullName: 'Maciej Janowski',
                    email: 'maciej@example.com',
                    phone: '+49 170 1234567',
                },
                headers: { 'set-cookie': 'token=fake; Path=/; HttpOnly' },
            });
        }).as('login');

        cy.visit('http://localhost:3000/login');
        cy.wait('@authCheck');

        cy.findByLabelText(/E-Mail/i).type('maciej@example.com');
        cy.findByLabelText(/Passwort/i, { selector: 'input' }).type('secret123');

        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 200,
            body: {
                username: 'maciej',
                role: 'ROLE_USER',
                fullName: 'Maciej Janowski',
                email: 'maciej@example.com',
                phone: '+49 170 1234567',
            },
        }).as('authCheckAfter');

        cy.findByRole('button', { name: /login/i }).click();

        cy.wait('@login');
        cy.wait('@authCheckAfter');

        cy.location('pathname', { timeout: 10000 }).should('eq', '/myaccount');
        cy.contains(/Maciej Janowski/i, { timeout: 10000 }).should('be.visible');
    });
});