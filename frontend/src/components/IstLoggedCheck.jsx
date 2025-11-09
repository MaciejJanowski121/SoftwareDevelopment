import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";

/** Basis-URL des Backends (kann über ENV-Variable gesetzt werden). */
const API = "http://localhost:8080";

/**
 * Wrapper-Komponente zur Prüfung, ob der Benutzer bereits eingeloggt ist.
 *
 * <p>Verhindert den Zugriff auf Seiten wie Login/Register, wenn bereits eine
 * gültige Session besteht, und leitet in diesem Fall nach <code>/myaccount</code>
 * weiter. Andernfalls werden die übergebenen Kinder gerendert.</p>
 *
 * <ul>
 *   <li>Fragt <code>/auth/auth_check</code> mit <code>credentials: "include"</code> ab.</li>
 *   <li>Zeigt während der Prüfung eine Ladeanzeige.</li>
 *   <li>Verwendet einen <em>AbortController</em>, um Race Conditions zu vermeiden.</li>
 * </ul>
 *
 * @component
 * @param {{ children: JSX.Element }} props
 * @returns {JSX.Element}
 */
export default function IsLoggedCheck({ children }) {
    /** Zustand: ob der Benutzer bereits eingeloggt ist. */
    const [isLogged, setIsLogged] = useState(false);
    /** Zustand: ob gerade die Prüfung läuft. */
    const [isLoading, setIsLoading] = useState(true);

    /**
     * Asynchrone Prüfung der Benutzer-Authentifizierung.
     *
     * <p>Ruft den Endpunkt <code>/auth/auth_check</code> auf und setzt
     * den lokalen Zustand abhängig vom Ergebnis.</p>
     *
     * @param {AbortSignal} signal
     * @returns {Promise<void>}
     */
    const checkAuth = async (signal) => {
        try {
            const res = await fetch(`${API}/auth/auth_check`, {
                method: "GET",
                credentials: "include",
                signal,
            });
            setIsLogged(res.ok);
        } catch {
            setIsLogged(false);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        const ctrl = new AbortController();
        checkAuth(ctrl.signal);
        return () => ctrl.abort();
    }, []);

    if (isLoading) {
        return <p aria-live="polite">Überprüfung der Anmeldung…</p>;
    }

    if (isLogged) {
        return <Navigate to="/myaccount" replace />;
    }

    return children;
}