// __tests__/ReservationForm.error.test.js
import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import ReservationForm from "../components/ReservationForm";

const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

// helper: jutro YYYY-MM-DD
const pad = (n) => (n < 10 ? "0" + n : "" + n);
const tomorrowDateStr = () => {
    const d = new Date(Date.now() + 24 * 60 * 60 * 1000);
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

test("zeigt error wenn die Reservierung nicht akzeptiert wird", async () => {
    // Mock fetch
    global.fetch = jest.fn().mockImplementation((input, init = {}) => {
        const url = typeof input === "string" ? input : input.url;
        const method = (init.method || "GET").toUpperCase();

        if (method === "GET" && /\/api\/reservations\/available/.test(url)) {
            return Promise.resolve({
                ok: true,
                headers: { get: () => "application/json" },
                json: async () => [{ id: 10, tableNumber: 3, numberOfSeats: 2 }],
            });
        }

        if (method === "POST" && /\/api\/reservations$/.test(url)) {
            // 400 — komponent wyrenderuje: "Reservierung fehlgeschlagen: Kapazität überschritten"
            return Promise.resolve({
                ok: false,
                status: 400,
                headers: { get: () => "text/plain" },
                text: async () => "Kapazität überschritten",
            });
        }

        return Promise.resolve({
            ok: true,
            headers: { get: () => "application/json" },
            json: async () => ({}),
            text: async () => "",
        });
    });

    const setReservation = jest.fn();
    renderWithRouter(<ReservationForm setReservation={setReservation} />);

    // data
    fireEvent.change(screen.getByLabelText(/Datum/i), {
        target: { value: tomorrowDateStr() },
    });

    // ⬇️ "Uhrzeit" nie ma htmlFor; wybierz dwa selecty z kontenera tuż po labelu
    const timeLabel = screen.getByText(/Uhrzeit/i);
    const timeRow = timeLabel.nextElementSibling; // <div class="time-row">...</div>
    const [hourSelect, minuteSelect] = within(timeRow).getAllByRole("combobox");
    fireEvent.change(hourSelect, { target: { value: "18" } });
    fireEvent.change(minuteSelect, { target: { value: "00" } });

    // Dauer
    fireEvent.change(screen.getByLabelText(/Dauer/i), { target: { value: "120" } });

    // poczekaj aż załadują się stoły i wybierz Tisch 3
    const tableSelect = screen.getByLabelText(/Tisch/i);
    await waitFor(() =>
        expect(within(tableSelect).getByRole("option", { name: /Tisch\s*3/i })).toBeInTheDocument()
    );
    fireEvent.change(tableSelect, { target: { value: "3" } });

    // submit
    fireEvent.click(screen.getByRole("button", { name: /Reservieren/i }));

    // oczekiwany błąd
    await waitFor(() => {
        expect(
            screen.getByText(/Reservierung fehlgeschlagen:\s*Kapazität überschritten/i)
        ).toBeInTheDocument();
    });
});