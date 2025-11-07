import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import React from 'react';

// Komponente zum Schutz von Seiten, die nur für authentifizierte Nutzer:innen zugänglich sind.
// Gibt das Kind-Element mit Benutzerinformationen weiter (username, role)

export default function Validation({ children }) {
    const navigate = useNavigate();

    const [isLoading, setIsLoading] = useState(true); // Status: wird Authentifizierung geprüft?
    const [authorized, setAuthorized] = useState(false); // Ist der Benutzer eingeloggt?
    const [username, setUsername] = useState(""); // Benutzername (falls eingeloggt)
    const [role, setRole] = useState(""); // Rolle: "ROLE_User",  "ROLE_ADMIN"

    useEffect(() => {
        let isMounted = true; // Schutz gegen Memory-Leaks bei unmount

        const checkAuth = async () => {
            try {
                const res = await fetch('http://localhost:8080/auth/auth_check', {
                    method: 'GET',
                    credentials: 'include', // JWT-Cookie wird gesendet
                });

                if (!isMounted) return;

                if (res.ok) {
                    const data = await res.json();
                    setUsername(data.username);
                    setRole(data.role);
                    setAuthorized(true);
                } else {
                    setAuthorized(false);
                }
            } catch (err) {
                setAuthorized(false);
            } finally {
                if (isMounted) setIsLoading(false); // Auth-Prüfung abgeschlossen
            }
        };

        checkAuth();

        // Cleanup bei Komponentendemontage
        return () => {
            isMounted = false;
        };
    }, []);

    // Während der Authentifizierungsprüfung
    if (isLoading) return <p>Authentifizierung wird überprüft...</p>;

    // Wenn autorisiert → Kindkomponente mit zusätzlichen Props zurückgeben
    const localUser = JSON.parse(localStorage.getItem("authUser") || "{}");

    return authorized && username
        ? React.cloneElement(children, { username, role })
        : localUser.username
            ? React.cloneElement(children, { username: localUser.username, role: localUser.role })
            : null;
}