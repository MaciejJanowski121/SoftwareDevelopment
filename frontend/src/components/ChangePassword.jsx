import { useNavigate } from "react-router-dom";
import { useState } from "react";
import "../styles/changepassword.css";

/**
 * Komponente zum Ändern des Benutzerpassworts.
 *
 * <p>Diese React-Komponente bietet ein Formular zur Änderung des Passworts
 * des aktuell eingeloggten Benutzers. Sie sendet eine Anfrage an den
 * Spring-Boot-Endpunkt <code>/user/change-password</code> und verwendet dabei
 * das JWT-Cookie für die Authentifizierung.</p>
 *
 * <ul>
 *   <li>Validiert Eingaben lokal (z. B. Mindestlänge des neuen Passworts).</li>
 *   <li>Zeigt Fehlermeldungen direkt im Formular an (ProblemDetail-Unterstützung).</li>
 *   <li>Leitet nach erfolgreicher Änderung zur Konto-Seite weiter.</li>
 * </ul>
 *
 * @component
 * @returns {JSX.Element} Password-Change-Formular.
 */
function ChangePassword() {
    const navigate = useNavigate();

    /** Zustand: altes Passwort */
    const [oldPassword, setOldPassword] = useState("");
    /** Zustand: neues Passwort */
    const [newPassword, setNewPassword] = useState("");
    /** Zustand: Fehlermeldung */
    const [error, setError] = useState("");
    /** Zustand: Loading-Indikator */
    const [isLoading, setIsLoading] = useState(false);

    /**
     * Sendet die Passwortänderung an das Backend.
     *
     * @async
     * @param {Event} e Submit-Event des Formulars
     */
    const handlePasswordChange = async (e) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);

        try {
            const response = await fetch("http://localhost:8080/user/change-password", {
                method: "PUT",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ oldPassword, newPassword }),
            });

            if (response.ok) {
                navigate("/myaccount");
                return;
            }

            let message = "Ein Fehler ist aufgetreten.";
            const ct = response.headers.get("content-type") || "";
            if (ct.includes("application/problem+json") || ct.includes("application/json")) {
                try {
                    const problem = await response.json();
                    message = problem?.detail || problem?.title || message;
                } catch {
                    message = await response.text().catch(() => message);
                }
            } else {
                message = await response.text().catch(() => message);
            }
            setError(message);
        } catch {
            setError("Netzwerk- oder Serverfehler.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="password-change-container">
            <form className="password-change-form" onSubmit={handlePasswordChange} noValidate>
                <button
                    type="button"
                    onClick={() => navigate("/myaccount")}
                    className="back-button"
                >
                    ← Zurück
                </button>

                <h1>Change Password</h1>

                <label htmlFor="oldPassword">Old Password</label>
                <input
                    id="oldPassword"
                    type="password"
                    placeholder="Enter old password"
                    autoComplete="current-password"
                    onChange={(e) => setOldPassword(e.target.value)}
                    required
                />

                <label htmlFor="newPassword">New Password</label>
                <input
                    id="newPassword"
                    type="password"
                    placeholder="Enter new password"
                    autoComplete="new-password"
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    minLength={6}
                />

                {error && (
                    <p className="error-message" aria-live="assertive">
                        {error}
                    </p>
                )}

                <button type="submit" className="password-change" disabled={isLoading}>
                    {isLoading ? "Processing..." : "Change Password"}
                </button>
            </form>
        </div>
    );
}

export default ChangePassword;