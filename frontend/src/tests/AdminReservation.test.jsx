import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import AdminReservations from "../pages/AdminReservations";
import { BrowserRouter } from "react-router-dom";

global.fetch = jest.fn();
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("AdminReservations", () => {
    beforeEach(() => fetch.mockClear());

    test("zeigt eine Liste von Reservierungen an", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            status: 200,
            headers: { get: () => "application/json" },
            json: async () => [
                {
                    id: 1,
                    username: "Maciej Janowski",
                    startTime: "2025-06-08T18:00:00",
                    endTime: "2025-06-08T20:00:00",
                    tableNumber: 5,
                },
            ],
        });

        renderWithRouter(<AdminReservations />);
        await waitFor(() =>
            expect(screen.getByText(/Maciej Janowski/i)).toBeInTheDocument()
        );
    });

    test("zeigt 'Keine Reservierungen gefunden.' bei leerer Antwort", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            status: 200,
            headers: { get: () => "application/json" },
            json: async () => [],
        });

        renderWithRouter(<AdminReservations />);
        await waitFor(() =>
            expect(
                screen.getByText(/Keine Reservierungen gefunden\./i)
            ).toBeInTheDocument()
        );
    });
});