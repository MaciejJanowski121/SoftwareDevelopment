// cypress/e2e/login.cy.js
describe('Login (E-Mail + Passwort)', () => {
    beforeEach(() => {
        // Cookies und LocalStorage vor jedem Test löschen,
        // um sicherzustellen, dass kein Benutzer eingeloggt bleibt
        cy.clearCookies();
        cy.clearLocalStorage();
    });

    it('loggt korrekt ein und navigiert zu /myaccount', () => {
        // 1) Beim Aufruf von /login ist der Benutzer nicht eingeloggt → 401
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 401,
            body: 'Invalid Token',
        }).as('authCheck');

        // 2) Stub für POST /auth/login (E-Mail + Passwort)
        cy.intercept('POST', '**/auth/login', (req) => {
            // Sicherstellen, dass der Request korrekt ist
            expect(req.headers['content-type']).to.include('application/json');
            expect(req.body).to.have.keys(['email', 'password']);
            expect(req.body.email).to.equal('maciej@example.com');

            // Simulierter erfolgreicher Login
            req.reply({
                statusCode: 200,
                body: {
                    username: 'maciej',
                    role: 'ROLE_USER',
                    fullName: 'Maciej Janowski',
                    email: 'maciej@example.com',
                    phone: '+49 170 1234567',
                },
                // Header müssen Strings sein – keine Arrays!
                headers: { 'set-cookie': 'token=fake; Path=/; HttpOnly' },
            });
        }).as('login');

        // 3) Besuch der Login-Seite
        cy.visit('http://localhost:3000/login');
        cy.wait('@authCheck'); // Warten auf die erste 401-Antwort

        // 4) Formular ausfüllen
        cy.findByLabelText(/E-Mail/i).type('maciej@example.com');
        cy.findByLabelText(/Passwort/i, { selector: 'input' }).type('secret123');

        // 5) Nach dem Klick auf "Login" wird erneut /auth_check aufgerufen → 200
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

        // Klick auf Login-Button
        cy.findByRole('button', { name: /login/i }).click();

        // 6) Überprüfen, dass alle API-Aufrufe korrekt waren
        cy.wait('@login');
        cy.wait('@authCheckAfter');

        // 7) Prüfen, ob der Benutzer zur MyAccount-Seite weitergeleitet wurde
// 7) MyAccount
        cy.location('pathname', { timeout: 10000 }).should('eq', '/myaccount');
        cy.contains(/Maciej Janowski/i, { timeout: 10000 }).should('be.visible');
// lub wersja ze stabilnym selektorem:
// cy.get('[data-cy="account-name"]', { timeout: 10000 }).should('contain', 'Maciej Janowski');", "maciej@example.com", itp.
    });
});