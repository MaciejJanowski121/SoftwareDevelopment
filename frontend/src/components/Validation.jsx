// src/components/Validation.jsx
import React, { useEffect, useState } from "react";

/**
 * Schützt Kinder-Komponenten durch einen einfachen Auth-Check.
 *
 * <p>Verhalten:</p>
 * <ul>
 *   <li>Ruft <code>/auth/auth_check</code> mit <code>credentials: "include"</code> auf.</li>
 *   <li>Bei Erfolg: setzt <code>authorized=true</code> und extrahiert <code>email</code>, <code>role</code>.</li>
 *   <li>Reicht diese Props per <code>React.cloneElement</code> an das Kind weiter.</li>
 *   <li>Optional: Fallback aus <code>localStorage</code> (Key: <code>authUser</code>) mit Feldern <code>email</code>, <code>role</code>.</li>
 * </ul>
 *
 * <p><strong>Hinweise:</strong></p>
 * <ul>
 *   <li>Das Backend liefert <code>AuthUserDTO</code> (&rarr; Felder: <code>email</code>, <code>role</code>, <code>fullName</code>, <code>phone</code>).</li>
 *   <li>Dieses HOC erwartet genau ein Kind (ein einzelnes React-Element).</li>
 * </ul>
 *
 * @param {{ children: React.ReactElement }} props
 */
export default function Validation({ children }) {
    // Ladezustand + Auth-Status
    const [isLoading, setIsLoading] = useState(true);
    const [authorized, setAuthorized] = useState(false);

    // Aus dem Backend: email (= Login-Identifier) und Rolle
    const [email, setEmail] = useState("");
    const [role, setRole] = useState("");

    useEffect(() => {
        // AbortController statt isMounted-Flag (saubereres Abbrechen von Fetches)
        const ac = new AbortController();

        const checkAuth = async () => {
            try {
                const res = await fetch("http://localhost:8080/auth/auth_check", {
                    method: "GET",
                    credentials: "include",
                    signal: ac.signal,
                });

                if (res.ok) {
                    const data = await res.json();
                    // Backend: AuthUserDTO -> { email, role, fullName, phone }
                    setEmail(data.email || "");
                    setRole(data.role || "");
                    setAuthorized(true);
                } else {
                    setAuthorized(false);
                }
            } catch {
                // Netzwerk-/Abbruch-Fehler => als nicht autorisiert behandeln
                setAuthorized(false);
            } finally {
                setIsLoading(false);
            }
        };

        checkAuth();
        return () => ac.abort();
    }, []);

    // Ladeanzeige während der Prüfung
    if (isLoading) {
        return <p aria-live="polite">Authentifizierung wird überprüft…</p>;
    }

    // Optionaler Fallback: Werte aus localStorage, falls vorhanden
    // Erwartetes Schema: { "email": "...", "role": "ROLE_..." }
    const localUser = (() => {
        try {
            return JSON.parse(localStorage.getItem("authUser") || "{}");
        } catch {
            return {};
        }
    })();

    // Wenn autorisiert und eine E-Mail vorhanden ist → Kind mit Props (email/role) anreichern
    if (authorized && email) {
        return React.cloneElement(children, { email, role });
    }

    // Fallback: lokaler Nutzer aus localStorage
    if (localUser?.email) {
        return React.cloneElement(children, {
            email: localUser.email,
            role: localUser.role,
        });
    }

    // Nicht autorisiert und kein Fallback → nichts rendern (oder tu hier optional <Navigate/>)
    return null;
}