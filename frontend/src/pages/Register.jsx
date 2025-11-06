import "../styles/loginAndRegister.css";
import { Link, useNavigate } from "react-router-dom";
import { useMemo, useState } from "react";

function Register() {
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [phone, setPhone] = useState("");
    const [password, setPassword] = useState("");

    const [showPwd, setShowPwd] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [successMsg, setSuccessMsg] = useState("");

    const navigate = useNavigate();
    const API = useMemo(
        () => process.env.REACT_APP_API_URL || "http://localhost:8080",
        []
    );

    const validEmail = (v) => /\S+@\S+\.\S+/.test(v);
    const validPhone = (v) => !v || /^(\+?\d{1,3}[\s-]?)?\d{6,15}$/.test(v.trim());
    async function handleSubmit(e) {
        e.preventDefault();
        setErrorMsg("");
        setSuccessMsg("");

        // einfache Client-Validierung
        if (fullName.trim().length < 2)
            return setErrorMsg("Bitte den vollständigen Namen angeben.");
        if (!validEmail(email))
            return setErrorMsg("Bitte eine gültige E-Mail-Adresse angeben.");
        if (!validPhone(phone))
            return setErrorMsg("Bitte eine gültige Telefonnummer angeben (oder leer lassen).");
        if (password.length < 6)
            return setErrorMsg("Passwort muss mindestens 6 Zeichen haben.");

        setSubmitting(true);

        try {
            const res = await fetch(`${API}/auth/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                // falls der Server noch `username` erwartet, verwenden wir die E-Mail
                body: JSON.stringify({
                    username: email,
                    email,
                    password,
                    fullName,
                    phone,
                }),
            });

            if (!res.ok) {
                let msg = "";
                // Versuche ProblemDetails (application/json) zu lesen
                try {
                    const ct = res.headers.get("content-type") || "";
                    if (ct.includes("application/json")) {
                        const j = await res.json();
                        // RFC7807 Felder bevorzugen
                        msg = j?.detail || j?.title || j?.message || "";

                        // optionale Feldfehler zusammenfassen (falls vorhanden)
                        if (!msg && j?.fields && typeof j.fields === "object") {
                            const parts = Object.entries(j.fields).map(
                                ([k, v]) => `${k}: ${String(v)}`
                            );
                            if (parts.length) msg = parts.join(", ");
                        }
                    } else {
                        // z.B. text/plain
                        const raw = await res.text();
                        if (raw) msg = raw;
                    }
                } catch {
                    // Ignorieren – weiter mit Status-Fallbacks
                }

                if (!msg) {
                    // Fallback nach Statuscode
                    switch (res.status) {
                        case 409:
                            msg = "Diese E-Mail ist bereits vergeben.";
                            break;
                        case 400:
                        case 422:
                            msg = "Eingaben sind ungültig. Bitte prüfen.";
                            break;
                        case 401:
                            msg = "Nicht autorisiert.";
                            break;
                        case 429:
                            msg = "Zu viele Versuche. Bitte später erneut versuchen.";
                            break;
                        case 500:
                            msg = "Serverfehler. Bitte später erneut versuchen.";
                            break;
                        default:
                            msg = "Registrierung fehlgeschlagen.";
                    }
                }

                throw new Error(msg);
            }

            // Erfolgspfad: Antwort ein einziges Mal als JSON lesen
            const data = await res.json();

            localStorage.setItem(
                "authUser",
                JSON.stringify({
                    username: data.username ?? email,
                    fullName: data.fullName ?? fullName,
                    email: data.email ?? email,
                    phone: data.phone ?? phone,
                    role: data.role ?? "ROLE_USER",
                })
            );

            setSuccessMsg("Registrierung erfolgreich! Weiterleitung...");
            setTimeout(() => navigate("/myaccount"), 1200);
        } catch (err) {
            setErrorMsg(err?.message || "Ein Fehler ist aufgetreten.");
        } finally {
            setSubmitting(false);
        }
    }

    const isDisabled =
        submitting ||
        fullName.trim().length < 2 ||
        !validEmail(email) ||
        password.length < 6;

    return (
        <main className="auth-page" aria-label="Registrierung">
            <section className="auth-card" aria-labelledby="register-title">
                <h1 id="register-title" className="auth-title">Registrieren</h1>

                {errorMsg && <div className="auth-alert" role="alert">{errorMsg}</div>}
                {successMsg && <div className="auth-success" role="status">{successMsg}</div>}

                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    <div className="field">
                        <label htmlFor="fullName">Vollständiger Name</label>
                        <input
                            id="fullName"
                            type="text"
                            autoComplete="name"
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                            minLength={2}
                            required
                        />
                        <small className="hint">Mindestens 2 Zeichen</small>
                    </div>

                    <div className="field">
                        <label htmlFor="email">E-Mail</label>
                        <input
                            id="email"
                            type="email"
                            autoComplete="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                        <small className="hint">Bitte gültige Adresse z. B. name@mail.de</small>
                    </div>

                    <div className="field">
                        <label htmlFor="phone">Telefon (optional)</label>
                        <input
                            id="phone"
                            type="tel"
                            autoComplete="tel"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            placeholder="+49 170 1234567"
                        />
                        <small className="hint">Nur Zahlen, +, - und Leerzeichen erlaubt</small>
                    </div>

                    <div className="field">
                        <label htmlFor="password">Passwort</label>
                        <div className="pwd-wrap">
                            <input
                                id="password"
                                type={showPwd ? "text" : "password"}
                                autoComplete="new-password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                minLength={6}
                                required
                            />
                            <button
                                type="button"
                                className="pwd-toggle"
                                onClick={() => setShowPwd((v) => !v)}
                                aria-label={showPwd ? "Passwort verbergen" : "Passwort anzeigen"}
                            >
                                {showPwd ? "Verbergen" : "Anzeigen"}
                            </button>
                        </div>
                        <small className="hint">Mindestens 6 Zeichen</small>
                    </div>

                    <button className="auth-btn" type="submit" disabled={isDisabled}>
                        {submitting ? <span className="spinner" aria-hidden="true" /> : null}
                        {submitting ? "Wird registriert…" : "Registrieren"}
                    </button>
                </form>

                <p className="auth-meta">
                    Bereits ein Konto?{" "}
                    <Link to="/login" className="auth-link">Jetzt einloggen</Link>
                </p>
            </section>
        </main>
    );
}

export default Register;