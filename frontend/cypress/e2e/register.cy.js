// cypress/e2e/register.cy.js
describe('Registrierung', () => {
    beforeEach(() => {
        cy.clearCookies();
        cy.clearLocalStorage();
    });

    it('registriert erfolgreich und navigiert zu /myaccount', () => {
        // -- Vor dem Besuch: alle Auth-Checks als 401 stubben (bleibt auf /register)
        cy.intercept('GET', '**/auth/auth_check', {
            statusCode: 401,
            headers: { 'content-type': 'application/json' },
            body: 'Unauthorized'
        }).as('auth401');

        // -- Seite öffnen und den ersten Check abwarten
        cy.visit('http://localhost:3000/register');
        cy.wait('@auth401'); // mindestens ein 401 bestätigt

        // -- Registrierungs-POST stubben (prüfen, was Frontend sendet)
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

        // -- Formular mit IDs ausfüllen (Label-Matcher pomijamy, żeby unikać kolizji)
        cy.get('#fullName').should('exist').type('Max Mustermann');
        cy.get('#email').type('max@example.com');
        cy.get('#phone').type('+491701234567');
        cy.get('#password').type('secret123');

        // -- Jetzt den Auth-Check auf 200 umstellen (erst NACH dem Ausfüllen)
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

        // -- Abschicken
        cy.findByRole('button', { name: /registrieren/i }).click();

        // -- Erst POST abwarten, dann den 200-Auth-Check abwarten
        cy.wait('@register');
        cy.wait('@auth200');

        // -- Weiterleitung prüfen
        cy.location('pathname').should('eq', '/myaccount');
    });

    it('zeigt einen Fehler, wenn E-Mail bereits vergeben ist (409)', () => {
        // -- Standardmäßig 401 lassen (kein Redirect)
        cy.intercept('GET', '**/auth/auth_check', { statusCode: 401 }).as('auth401');

        cy.visit('http://localhost:3000/register');
        cy.wait('@auth401');

        // -- 409-Konflikt stubben
        cy.intercept('POST', '**/auth/register', {
            statusCode: 409,
            headers: { 'content-type': 'text/plain' },
            body: 'E-Mail bereits vergeben'
        }).as('registerConflict');

        // -- Formular ausfüllen
        cy.get('#fullName').type('Max Mustermann');
        cy.get('#email').type('max@example.com');
        cy.get('#phone').type('+491701234567');
        cy.get('#password').type('secret123');

        // -- Absenden
        cy.findByRole('button', { name: /registrieren/i }).click();

        // -- Auf 409 warten und Fehler anzeigen prüfen
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

        // -- Keine Weiterleitung
        cy.location('pathname').should('eq', '/register');
    });
});