import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, useNavigate } from "react-router-dom";
import AdminPanel from "../pages/AdminPanel";

// Mock von useNavigate
jest.mock("react-router-dom", () => {
    const original = jest.requireActual("react-router-dom");
    return {
        ...original,
        useNavigate: jest.fn(),
    };
});

describe("AdminPanel", () => {
    const mockNavigate = jest.fn();

    beforeEach(() => {
        useNavigate.mockReturnValue(mockNavigate);
        jest.clearAllMocks();
    });

    test("zeigt Admin-Inhalte, wenn die Rolle ROLE_ADMIN ist", async () => {
        render(
            <MemoryRouter>
                <AdminPanel username="Maciej" role="ROLE_ADMIN" />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/Admin Panel/i)).toBeInTheDocument();
            expect(screen.getByText(/Angemeldet als:/i)).toBeInTheDocument();
            expect(screen.getByText(/Maciej/i)).toBeInTheDocument();
        });

        expect(mockNavigate).not.toHaveBeenCalled();
    });

    test("leitet weiter nach /myaccount, wenn Rolle nicht ROLE_ADMIN ist", () => {
        render(
            <MemoryRouter>
                <AdminPanel username="Maciej" role="ROLE_USER" />
            </MemoryRouter>
        );
        expect(mockNavigate).toHaveBeenCalledTimes(1);
        expect(mockNavigate).toHaveBeenCalledWith("/myaccount");
    });

    test("leitet weiter, wenn keine Rolle vorhanden ist", () => {
        render(
            <MemoryRouter>
                <AdminPanel username="Maciej" />
            </MemoryRouter>
        );
        expect(mockNavigate).toHaveBeenCalledWith("/myaccount");
    });
});