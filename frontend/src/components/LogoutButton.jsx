import { useNavigate } from "react-router-dom";

/**
 * Button-Komponente zum Abmelden des aktuell eingeloggten Benutzers.
 *
 * <p>Sendet eine POST-Anfrage an <code>http://localhost:8080/auth/logout</code>,
 * um das Authentifizierungs-Cookie (JWT) zu löschen. Nach erfolgreichem Logout
 * erfolgt eine automatische Weiterleitung zur Login-Seite.</p>
 *
 * <ul>
 *   <li>Verwendet <code>credentials: "include"</code>, damit das Cookie mitgesendet wird.</li>
 *   <li>Bei Fehlern wird eine einfache Fehlermeldung im Browser angezeigt.</li>
 *   <li>Die visuelle Gestaltung (CSS) kann über die <code>className</code>-Prop angepasst werden.</li>
 * </ul>
 *
 * @component
 * @param {Object} props - React-Komponenten-Props.
 * @param {string} [props.className] - Optionaler CSS-Klassenname für den Button.
 * @returns {JSX.Element} - Der Logout-Button mit Backend-Integration.
 */
function LogoutButton({ className }) {
    const navigate = useNavigate(); // Navigation nach erfolgreichem Logout

    /** Führt die Abmeldung durch, indem das JWT-Cookie gelöscht wird. */
    const handleLogout = async () => {
        try {
            const res = await fetch("http://localhost:8080/auth/logout", {
                method: "POST",
                credentials: "include", // JWT-Cookie wird mitgesendet
            });

            if (res.ok) {
                // Erfolgreicher Logout → Weiterleitung zur Login-Seite
                navigate("/login");
            } else {
                alert("Logout fehlgeschlagen");
            }
        } catch (err) {
            console.error("Fehler beim Logout:", err);
        }
    };

    /** Button zum Ausloggen; Klasse kann per Prop übergeben werden. */
    return (
        <button onClick={handleLogout} className={className}>
            Logout
        </button>
    );
}

export default LogoutButton;