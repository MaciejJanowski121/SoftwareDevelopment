import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Login from "../pages/Login";
import { BrowserRouter } from "react-router-dom";

const mockedUsedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => mockedUsedNavigate,
}));

global.fetch = jest.fn();

const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

/**
 * Integrationstest für die Login-Seite.
 *
 * <p>Dieser Test überprüft das Verhalten der Komponente
 * <code>Login.jsx</code> in verschiedenen Authentifizierungsszenarien.</p>
 *
 * <ul>
 *   <li>Mockt die Fetch-API für erfolgreiche und fehlgeschlagene Logins.</li>
 *   <li>Verwendet <code>BrowserRouter</code> und eine gemockte <code>useNavigate</code>-Funktion,
 *       um Navigationen zu /admin oder /myaccount zu testen.</li>
 *   <li>Prüft folgende Fälle:
 *     <ul>
 *       <li>Rendering der Eingabefelder und Buttons,</li>
 *       <li>Aktualisierung der Eingaben (E-Mail und Passwort),</li>
 *       <li>Erfolgreiche Anmeldung und Weiterleitung für <code>ROLE_ADMIN</code>,</li>
 *       <li>Erfolgreiche Anmeldung und Weiterleitung für <code>ROLE_USER</code>,</li>
 *       <li>Anzeige einer Fehlermeldung bei falschen Zugangsdaten.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @component
 * @returns {void} Führt automatisierte UI-Tests mit React Testing Library aus.
 */
describe("Login", () => {
    beforeEach(() => {
        fetch.mockClear();
        mockedUsedNavigate.mockClear();
    });

    test("rendert Felder und Button", () => {
        renderWithRouter(<Login />);
        expect(screen.getByLabelText(/^E-Mail$/i)).toBeInTheDocument();
        expect(
            screen.getByLabelText(/^Passwort$/i, { selector: "input" })
        ).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /Login/i })).toBeInTheDocument();
    });

    test("Aktualisiert Felder", () => {
        renderWithRouter(<Login />);
        const emailInput = screen.getByLabelText(/^E-Mail$/i);
        const passwordInput = screen.getByLabelText(/^Passwort$/i, {
            selector: "input",
        });

        fireEvent.change(emailInput, { target: { value: "testuser@example.com" } });
        fireEvent.change(passwordInput, { target: { value: "secret" } });

        expect(emailInput.value).toBe("testuser@example.com");
        expect(passwordInput.value).toBe("secret");
    });

    test("submit und Navigation zu /admin mit ROLE_ADMIN", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ username: "adminuser", role: "ROLE_ADMIN" }),
        });

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/^E-Mail$/i), {
            target: { value: "admin@example.com" },
        });
        fireEvent.change(
            screen.getByLabelText(/^Passwort$/i, { selector: "input" }),
            { target: { value: "adminpass" } }
        );

        fireEvent.click(screen.getByRole("button", { name: /Login/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalled();
            expect(mockedUsedNavigate).toHaveBeenCalledWith("/admin");
        });
    });

    test("submit i nawigacja do /myaccount przy ROLE_USER", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ username: "regularuser", role: "ROLE_USER" }),
        });

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/^E-Mail$/i), {
            target: { value: "user@example.com" },
        });
        fireEvent.change(
            screen.getByLabelText(/^Passwort$/i, { selector: "input" }),
            { target: { value: "userpass" } }
        );

        fireEvent.click(screen.getByRole("button", { name: /Login/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalled();
            expect(mockedUsedNavigate).toHaveBeenCalledWith("/myaccount");
        });
    });

    test("zeigt Meldung, wenn falsche Anmeldung", async () => {
        const errorText = "Invalid credentials";
        fetch.mockResolvedValueOnce({ ok: false, text: async () => errorText });

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/^E-Mail$/i), {
            target: { value: "fail@example.com" },
        });
        fireEvent.change(
            screen.getByLabelText(/^Passwort$/i, { selector: "input" }),
            { target: { value: "failpass" } }
        );

        fireEvent.click(screen.getByRole("button", { name: /Login/i }));

        await waitFor(() => {
            expect(screen.getByRole("alert")).toHaveTextContent(
                /Invalid credentials|Login fehlgeschlagen/i
            );
        });
    });
});