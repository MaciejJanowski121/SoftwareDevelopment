import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import Reservations from "../pages/Reservations";
import { BrowserRouter } from "react-router-dom";

const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

/**
 * Integrationstest für die Seite <code>Reservations.jsx</code>.
 *
 * <p>Dieser Test überprüft die Anzeige und das Löschen einer Benutzer-Reservierung.</p>
 *
 * <ul>
 *   <li>Mockt die API-Aufrufe:
 *     <ul>
 *       <li><code>GET /api/reservations/userReservations</code> – liefert bestehende Reservierung,</li>
 *       <li><code>DELETE /api/reservations/:id</code> – simuliert erfolgreiche Löschung.</li>
 *     </ul>
 *   </li>
 *   <li>Prüft, dass:
 *     <ul>
 *       <li>die Reservierung mit Benutzer, Tisch-Nr. und Uhrzeit korrekt angezeigt wird,</li>
 *       <li>nach Klick auf „Reservierung löschen“ der entsprechende DELETE-Request ausgeführt wird.</li>
 *     </ul>
 *   </li>
 *   <li>Validiert die Integration von Datenabruf, UI-Darstellung und Benutzerinteraktion.</li>
 * </ul>
 *
 * @component
 * @returns {void} Führt automatisierte UI-Tests mit React Testing Library aus.
 */
describe("Reservations", () => {
    beforeEach(() => {
        global.fetch = jest.fn();
        jest.clearAllMocks();
    });

    test("pokazuje rezerwację użytkownika", async () => {
        const dto = {
            id: 1,
            username: "maciej",
            tableNumber: 7,
            startTime: "2025-06-09T18:00:00",
            endTime: "2025-06-09T20:00:00",
        };

        fetch.mockResolvedValueOnce({
            ok: true,
            status: 200,
            headers: { get: () => "application/json" },
            text: async () => JSON.stringify(dto),
        });

        renderWithRouter(<Reservations />);

        await waitFor(() => {
            expect(screen.getByText(/Benutzer:/i)).toBeInTheDocument();
            expect(screen.getByText("7", { exact: false })).toBeInTheDocument();
            expect(screen.getByText(/18:00 – 20:00/)).toBeInTheDocument();
        });
    });

    test("usuwa rezerwację po kliknięciu", async () => {
        const dto = {
            id: 2,
            username: "anna",
            tableNumber: 3,
            startTime: "2025-06-10T19:00:00",
            endTime: "2025-06-10T21:00:00",
        };

        fetch
            .mockResolvedValueOnce({
                ok: true,
                status: 200,
                headers: { get: () => "application/json" },
                text: async () => JSON.stringify(dto),
            })
            .mockResolvedValueOnce({ ok: true, status: 200, text: async () => "" });

        renderWithRouter(<Reservations />);

        await waitFor(() =>
            expect(
                screen.getByRole("button", { name: /Reservierung löschen/i })
            ).toBeInTheDocument()
        );

        fireEvent.click(screen.getByRole("button", { name: /Reservierung löschen/i }));

        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/api/reservations/2",
                expect.objectContaining({ method: "DELETE" })
            )
        );
    });
});