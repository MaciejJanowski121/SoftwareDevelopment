import "../styles/loginAndRegister.css";
import { Link, useNavigate } from "react-router-dom";
import { useMemo, useState } from "react";

/**
 * Registrierungsseite für neue Benutzer.
 *
 * <p>Rendert ein Formular mit Feldern <code>fullName</code>, <code>email</code>,
 * <code>phone</code> (optional) und <code>password</code>. Führt einfache
 * Client-Validierungen durch (z. B. Mindestlängen, E-Mail-/Telefon-Format) und
 * sendet die Daten an <code>POST /auth/register</code> mit <code>credentials: "include"</code>
 * (JWT-Cookie vom Backend).</p>
 *
 * <ul>
 *   <li>Zeigt Serverfehler strukturiert an (ProblemDetail, 409/400 usw.).</li>
 *   <li>Speichert das Ergebnis in <code>localStorage</code> (Schlüssel <code>authUser</code>).</li>
 *   <li>Leitet bei Erfolg automatisch zu <code>/myaccount</code> weiter.</li>
 * </ul>
 *
 * @component
 * @returns {JSX.Element}
 */
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
    const API = useMemo(() => process.env.REACT_APP_API_URL || "http://localhost:8080", []);

    const validEmail = (v) => /\S+@\S+\.\S+/.test(v);
    const validPhone = (v) => !v || /^[-+()\s0-9]{6,20}$/.test(v.trim());

    async function handleSubmit(e) {
        e.preventDefault();
        setErrorMsg("");
        setSuccessMsg("");

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
                body: JSON.stringify({
                    email,
                    password,
                    fullName,
                    phone,
                }),
            });

            if (!res.ok) {
                let msg = "";
                try {
                    const text = await res.text();
                    if (text) {
                        const j = JSON.parse(text);
                        const fieldMsg =
                            j?.fields && typeof j.fields === "object"
                                ? Object.values(j.fields)[0]
                                : "";
                        msg = fieldMsg || j.detail || j.title || j.message || "";
                    }
                } catch {}

                if (!msg) {
                    switch (res.status) {
                        case 409:
                            msg = "Diese E-Mail ist bereits vergeben.";
                            break;
                        case 400:
                            msg = "Ungültige Eingaben.";
                            break;
                        default:
                            msg = "Registrierung fehlgeschlagen.";
                    }
                }

                setErrorMsg(msg);
                return;
            }

            const data = await res.json();

            localStorage.setItem(
                "authUser",
                JSON.stringify({
                    username: data.username ?? data.email ?? email,
                    fullName: data.fullName ?? fullName,
                    email: data.email ?? email,
                    phone: data.phone ?? phone,
                    role: data.role ?? "ROLE_USER",
                })
            );

            setSuccessMsg("Registrierung erfolgreich! Weiterleitung...");
            setTimeout(() => navigate("/myaccount"), 900);
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

                {errorMsg && (
                    <div className="auth-alert" role="alert" aria-live="assertive" data-cy="register-error">
                        {errorMsg}
                    </div>
                )}
                {successMsg && (
                    <div className="auth-success" role="status" aria-live="polite" data-cy="register-success">
                        {successMsg}
                    </div>
                )}

                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    <div className="field">
                        <label htmlFor="fullName">Vollständiger Name</label>
                        <input
                            id="fullName"
                            name="fullName"
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
                            name="email"
                            type="email"
                            autoComplete="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            onBlur={() => setEmail((v) => v.trim().toLowerCase())}
                            required
                        />
                        <small className="hint">Bitte gültige Adresse z. B. name@mail.de</small>
                    </div>

                    <div className="field">
                        <label htmlFor="phone">Telefon (optional)</label>
                        <input
                            id="phone"
                            name="phone"
                            type="tel"
                            inputMode="tel"
                            pattern="[-+()\s0-9]{6,20}"
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
                                name="password"
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