import { NavLink, useLocation } from "react-router-dom";
import { useEffect, useMemo, useState } from "react";
import "../styles/header.css";

/**
 * Navigationskopf der Anwendung.
 *
 * <p>Diese Komponente zeigt das Marken-Logo und kontextabhängige Navigationslinks.
 * Sie prüft beim Routenwechsel den Login-Status über <code>/auth/auth_check</code>
 * (JWT-Cookie) und blendet Menüpunkte für Gäste, Benutzer oder Admins ein.</p>
 *
 * <ul>
 *   <li>Verwendet <code>NavLink</code> für automatische "active"-Klassen.</li>
 *   <li>Verwendet <em>AbortController</em>, um Race Conditions bei schnellen Routenwechseln zu vermeiden.</li>
 *   <li>Hält einen Ladezustand (<code>isLoading</code>) für saubere UI.</li>
 * </ul>
 *
 * @component
 * @returns {JSX.Element} Navigationsleiste der Anwendung.
 */
function Header() {
    const location = useLocation();
    const API = useMemo(() => "http://localhost:8080", []);

    /** Zustand: ob der Benutzer eingeloggt ist. */
    const [isLogged, setIsLogged] = useState(false);
    /** Zustand: Rolle des Benutzers (z. B. "ROLE_ADMIN"). */
    const [role, setRole] = useState(null);
    /** Zustand: Ladeindikator für den Auth-Check. */
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const ctrl = new AbortController();

        (async () => {
            if (!ctrl.signal.aborted) setIsLoading(true);
            try {
                const res = await fetch(`${API}/auth/auth_check`, {
                    credentials: "include",
                    signal: ctrl.signal,
                });

                if (ctrl.signal.aborted) return;

                if (!res.ok) {
                    setIsLogged(false);
                    setRole(null);
                    return;
                }

                const data = await res.json();
                setIsLogged(true);
                setRole(data?.role ?? null);
            } catch {
                if (!ctrl.signal.aborted) {
                    setIsLogged(false);
                    setRole(null);
                }
            } finally {
                if (!ctrl.signal.aborted) setIsLoading(false);
            }
        })();

        return () => ctrl.abort();
    }, [API, location.pathname]);

    return (
        <header className="navbar">
            <div className="navbar-container">
                <div className="navbar-logo">
                    <NavLink to="/" className="brand">
                        Restaurant
                    </NavLink>
                </div>

                <nav className="navbar-links">
                    <NavLink
                        to="/"
                        className={({ isActive }) => (isActive ? "active" : undefined)}
                        end
                    >
                        Home
                    </NavLink>

                    {!isLoading && !isLogged && (
                        <>
                            <NavLink
                                to="/register"
                                className={({ isActive }) => (isActive ? "active" : undefined)}
                            >
                                Register
                            </NavLink>
                            <NavLink
                                to="/login"
                                className={({ isActive }) => (isActive ? "active" : undefined)}
                            >
                                Login
                            </NavLink>
                        </>
                    )}

                    {!isLoading && isLogged && role !== "ROLE_ADMIN" && (
                        <NavLink
                            to="/myaccount"
                            className={({ isActive }) => (isActive ? "active" : undefined)}
                        >
                            My Account
                        </NavLink>
                    )}

                    {!isLoading && isLogged && role === "ROLE_ADMIN" && (
                        <NavLink
                            to="/admin"
                            className={({ isActive }) => (isActive ? "active" : undefined)}
                        >
                            Admin Panel
                        </NavLink>
                    )}
                </nav>
            </div>
        </header>
    );
}

export default Header;