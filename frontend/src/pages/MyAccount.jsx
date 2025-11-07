import '../styles/myaccount.css';
import { Link } from "react-router-dom";
import { useMemo } from "react";
import LogoutButton from "../components/LogoutButton";

function MyAccount(props) {
    // 1) Fallback na localStorage, jeśli props nie przyszły
    const stored = useMemo(() => {
        try {
            return JSON.parse(localStorage.getItem("authUser") || "{}");
        } catch {
            return {};
        }
    }, []);

    const username = props?.username ?? stored?.username ?? stored?.email ?? "";
    const role = props?.role ?? stored?.role ?? "";
    const fullName = stored?.fullName ?? "";

    // 2) Co pokazać jako nagłówek
    const displayName = fullName || username || "Benutzer";
    const initial =
        (fullName?.trim()?.charAt(0)) ||
        (username?.trim()?.charAt(0)) ||
        "U";

    return (
        <div className="account-container">
            <div className="account-box">
                {/* Header */}
                <div className="account-header">
                    <div className="account-avatar">
                        {String(initial).toUpperCase()}
                    </div>
                    <h1>{displayName}</h1>
                    <p className="account-subtitle">
                        Willkommen in deinem Benutzerbereich
                    </p>
                </div>

                {/* Sekcja akcji – tylko dla nie-adminów */}
                {role !== "ROLE_ADMIN" ? (
                    <div className="account-actions">
                        <Link to="/reservations/new" className="account-btn account-btn--gold">
                            Neue Reservierung
                        </Link>

                        <Link to="/reservations/my" className="account-btn account-btn--blue">
                            Meine Reservierungen
                        </Link>

                        <Link to="/changePassword" className="account-btn account-btn--blue">
                            Passwort ändern
                        </Link>

                        <LogoutButton className="account-btn account-btn--danger" />
                    </div>
                ) : (
                    <div className="account-actions">
                        <p className="account-note">
                            Du bist als <strong>Admin</strong> angemeldet. Nutze das Admin-Panel.
                        </p>
                        <Link to="/admin" className="account-btn account-btn--blue">
                            Zum Admin-Panel
                        </Link>
                        <LogoutButton className="account-btn account-btn--danger" />
                    </div>
                )}
            </div>
        </div>
    );
}

export default MyAccount;