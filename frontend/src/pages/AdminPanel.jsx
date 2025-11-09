
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import LogoutButton from "../components/LogoutButton";
import "../styles/adminpanel.css";

/**
 * Administrationsbereich für Benutzer mit der Rolle <code>ROLE_ADMIN</code>.
 *
 * <p>Funktionen:</p>
 * <ul>
 *   <li>Überprüft die Benutzerrolle beim Rendern und leitet nicht berechtigte Benutzer
 *       automatisch auf <code>/myaccount</code> weiter.</li>
 *   <li>Zeigt den eingeloggten Admin-Namen an und bietet Zugriff auf Verwaltungsfunktionen.</li>
 *   <li>Enthält eine Navigation zur Seite „Alle Reservierungen“ sowie einen Logout-Button.</li>
 * </ul>
 *
 * <p><strong>Verwendung:</strong> Diese Seite wird nur angezeigt, wenn
 * <code>role === "ROLE_ADMIN"</code>. Sie dient als Einstiegspunkt für
 * administrative Aktionen im System.</p>
 *
 * @component
 * @param {{ username: string, role: string }} props - Vom Auth-Wrapper übergebene Benutzerinformationen.
 * @returns {JSX.Element}
 */
function AdminPanel({ username, role }) {
    const navigate = useNavigate();

    useEffect(() => {
        if (role !== "ROLE_ADMIN") {
            navigate("/myaccount");
        }
    }, [role, navigate]);

    return (
        <div className="admin-container">
            <div className="admin-box">
                <h1>Admin Panel</h1>
                <p>Willkommen im Adminbereich.</p>
                <p>
                    Angemeldet als: <strong>{username}</strong>
                </p>

                <div className="admin-actions">
                    <button
                        onClick={() => navigate("/admin/reservations")}
                        className="account-btn account-btn--primary"
                    >
                        Alle Reservierungen anzeigen
                    </button>

                    <LogoutButton className="account-btn account-btn--danger" />
                </div>
            </div>
        </div>
    );
}

export default AdminPanel;