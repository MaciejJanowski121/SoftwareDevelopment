
import React from "react";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import AdminReservations from "../pages/AdminReservations";

/**
 * Integrationstest für die Admin-Reservierungsseite.
 *
 * <p>Dieser Test überprüft die Korrektheit der Komponente
 * <code>AdminReservations.jsx</code> unter verschiedenen Bedingungen.</p>
 *
 * <ul>
 *   <li>Mockt Backend-Antworten über <code>global.fetch</code>.</li>
 *   <li>Verwendet <code>BrowserRouter</code>, um die Router-Umgebung zu simulieren.</li>
 *   <li>Testet drei Hauptszenarien:
 *     <ul>
 *       <li>Anzeige einer Liste von Reservierungen,</li>
 *       <li>Anzeige der Meldung bei leerer Antwort,</li>
 *       <li>Anzeige einer Fehlermeldung bei fehlenden Rechten (401/403).</li>
 *     </ul>
 *   </li>
 *   <li>Verwendet ARIA-Rollen (<code>list</code>, <code>listitem</code>),
 *       um UI-Elemente stabil zu identifizieren.</li>
 * </ul>
 *
 * @component
 * @returns {void} Führt automatisierte Tests mit React Testing Library aus.
 */
describe("AdminReservations", () => {
    const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

    beforeEach(() => {
        global.fetch = jest.fn();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    test("zeigt eine Liste von Reservierungen an", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            status: 200,
            headers: { get: () => "application/json" },
            json: async () => [
                {
                    id: 1,
                    fullName: "Maciej Janowski", // ← KLUCZOWA ZMIANA
                    startTime: "2025-06-08T18:00:00",
                    endTime: "2025-06-08T20:00:00",
                    tableNumber: 5,
                },
            ],
            text: async () => "",
        });

        renderWithRouter(<AdminReservations />);

        await screen.findByRole("heading", { name: /alle reservierungen/i });

        const items = await screen.findAllByRole("listitem");
        expect(items[0]).toHaveTextContent(/Maciej Janowski/i);
        expect(items[0]).toHaveTextContent(/Tisch/i);
    });
    test("zeigt 'Keine Reservierungen gefunden.' bei leerer Antwort", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            status: 200,
            headers: { get: () => "application/json" },
            json: async () => [],
            text: async () => "",
        });

        renderWithRouter(<AdminReservations />);
        await screen.findByRole("heading", { name: /alle reservierungen/i });
        expect(
            await screen.findByText(/Keine Reservierungen gefunden\./i)
        ).toBeInTheDocument();
    });

    test("zeigt Fehlermeldung bei fehlenden Rechten (401/403)", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: false,
            status: 401,
            headers: { get: () => "text/plain" },
            text: async () => "Unauthorized",
        });

        renderWithRouter(<AdminReservations />);

        // komponent mapuje 401/403 na polski komunikat lub zwraca treść z backendu
        const err = await screen.findByText(/Brak uprawnień|Unauthorized/i);
        expect(err).toBeInTheDocument();
    });
});