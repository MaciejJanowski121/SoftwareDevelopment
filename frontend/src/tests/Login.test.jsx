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
            expect(fetch).toHaveBeenCalled(); // nie blokujemy się na konkretnym body
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

        // Twój komponent pokazuje komunikat (np. <div role="alert">…</div>) – asercja na to:
        await waitFor(() => {
              expect(screen.getByRole("alert"))
                .toHaveTextContent(/Invalid credentials|Login fehlgeschlagen/i);
            });
    });
});