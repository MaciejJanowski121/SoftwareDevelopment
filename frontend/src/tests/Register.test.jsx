import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Register from "../pages/Register";
import { BrowserRouter } from "react-router-dom";

global.fetch = jest.fn();
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

/**
 * Integrationstest für die Registrierungsseite.
 *
 * <p>Dieser Test überprüft das Verhalten der Komponente
 * <code>Register.jsx</code> während des vollständigen Registrierungsablaufs.</p>
 *
 * <ul>
 *   <li>Mockt die <code>fetch</code>-API, um eine erfolgreiche Serverantwort zu simulieren.</li>
 *   <li>Füllt das Formular mit Testdaten aus und sendet es ab.</li>
 *   <li>Überprüft, dass die Anfrage korrekt an <code>/auth/register</code> gesendet wird
 *       und eine Erfolgsnachricht mit <code>role="status"</code> angezeigt wird.</li>
 *   <li>Validiert die Integration von Frontend-Formularlogik und Backend-Kommunikation.</li>
 * </ul>
 *
 * @component
 * @returns {void} Führt automatisierten Integrationstest mit React Testing Library aus.
 */
describe("Register", () => {
    beforeEach(() => fetch.mockClear());

    test("sendet Formular und zeigt Erfolg", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                fullName: "Maciej Janowski",
                email: "maciej@example.com",
                phone: "+491701234567",
            }),
        });

        renderWithRouter(<Register />);

        fireEvent.change(screen.getByLabelText(/Vollständiger Name/i), {
            target: { value: "Maciej Janowski" },
        });
        fireEvent.change(screen.getByLabelText(/^E-Mail$/i), {
            target: { value: "maciej@example.com" },
        });
        fireEvent.change(screen.getByLabelText(/Telefon/i), {
            target: { value: "+491701234567" },
        });
        fireEvent.change(screen.getByLabelText(/^Passwort$/i), {
            target: { value: "secret1" },
        });

        fireEvent.click(screen.getByRole("button", { name: /Registrieren/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/auth/register",
                expect.objectContaining({
                    method: "POST",
                    credentials: "include",
                    headers: expect.objectContaining({
                        "Content-Type": "application/json",
                    }),
                })
            );
            expect(screen.getByRole("status")).toHaveTextContent(/Registrierung erfolgreich/i);
        });
    });
});