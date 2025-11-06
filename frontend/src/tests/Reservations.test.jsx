import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import Reservations from "../pages/Reservations";
import { BrowserRouter } from "react-router-dom";

const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

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