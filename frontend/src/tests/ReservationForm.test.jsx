// __tests__/ReservationForm.error.test.js
import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import ReservationForm from "../components/ReservationForm";


/**
 * Integrationstest für das Reservierungsformular (Fehlerpfad).
 *
 * <p>Prüft, dass die Komponente bei einer fehlgeschlagenen Reservierung
 * (HTTP 400) eine aussagekräftige Fehlermeldung rendert.</p>
 *
 * <ul>
 *   <li>Mockt <code>GET /api/reservations/available</code> (verfügbare Tische).</li>
 *   <li>Mockt <code>POST /api/reservations</code> mit Status 400 und Klartext-Fehler.</li>
 *   <li>Füllt Datum, Uhrzeit, Dauer und Tisch aus und sendet das Formular ab.</li>
 *   <li>Erwartet die Meldung: „Reservierung fehlgeschlagen: Kapazität überschritten“.</li>
 * </ul>
 *
 * @remarks
 * Die Uhrzeit-Selects werden über den Container nach dem Label „Uhrzeit“
 * mittels <code>within(...).getAllByRole('combobox')</code> selektiert.
 *
 * @returns {void}
 */

const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);
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


    fireEvent.change(screen.getByLabelText(/Datum/i), {
        target: { value: tomorrowDateStr() },
    });


    const timeLabel = screen.getByText(/Uhrzeit/i);
    const timeRow = timeLabel.nextElementSibling;
    const [hourSelect, minuteSelect] = within(timeRow).getAllByRole("combobox");
    fireEvent.change(hourSelect, { target: { value: "18" } });
    fireEvent.change(minuteSelect, { target: { value: "00" } });

    // Dauer
    fireEvent.change(screen.getByLabelText(/Dauer/i), { target: { value: "120" } });


    const tableSelect = screen.getByLabelText(/Tisch/i);
    await waitFor(() =>
        expect(within(tableSelect).getByRole("option", { name: /Tisch\s*3/i })).toBeInTheDocument()
    );
    fireEvent.change(tableSelect, { target: { value: "3" } });

    // submit
    fireEvent.click(screen.getByRole("button", { name: /Reservieren/i }));


    await waitFor(() => {
        expect(
            screen.getByText(/Reservierung fehlgeschlagen:\s*Kapazität überschritten/i)
        ).toBeInTheDocument();
    });
});