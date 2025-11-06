import "../styles/loginAndRegister.css";
import { Link, useNavigate } from "react-router-dom";
import { useState, useMemo } from "react";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPwd, setShowPwd] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    const navigate = useNavigate();
    const API = useMemo(() => process.env.REACT_APP_API_URL || "http://localhost:8080", []);

    async function handleSubmit(e) {
        e.preventDefault();
        setErrorMsg("");
        setSubmitting(true);

        try {
            const res = await fetch(`${API}/auth/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ email, password }),
            });

            if (!res.ok) {
                let msg = "";
                // spróbuj JSON (ProblemDetail / JSON z backendu)
                try {
                    const ct = res.headers.get("content-type") || "";
                    if (ct.includes("application/json")) {
                        const j = await res.json();
                        // RFC7807: detail/title; fallback na message
                        msg = j?.detail || j?.title || j?.message || "";
                        // walidacja: możesz wyciągnąć np. j.fields i złożyć komunikat
                        // if (j?.fields && typeof j.fields === "object") { ... }
                    }
                } catch {
                    // ignorujemy – spróbujemy fallbacków niżej
                }

                if (!msg) {
                    // fallback po statusie (gdy brak JSON)
                    switch (res.status) {
                        case 401:
                        case 404:
                            msg = "E-Mail oder Passwort ist falsch.";
                            break;
                        case 429:
                            msg = "Zu viele Versuche. Bitte später erneut versuchen.";
                            break;
                        case 500:
                            msg = "Serverfehler. Bitte später erneut versuchen.";
                            break;
                        default:
                            msg = "Login fehlgeschlagen.";
                    }
                }

                throw new Error(msg);
            }

            // OK -> czytamy JSON raz
            const data = await res.json();

            localStorage.setItem(
                "authUser",
                JSON.stringify({
                    username: data.username, // u Ciebie to e-mail
                    role: data.role,
                    fullName: data.fullName,
                    email: data.email,
                    phone: data.phone,
                })
            );

            if (data.role === "ROLE_ADMIN") navigate("/admin");
            else navigate("/myaccount");
        } catch (err) {
            setErrorMsg(err?.message || "Ein Fehler ist aufgetreten.");
        } finally {
            setSubmitting(false);
        }
    }
    const isDisabled = submitting || !email.trim() || !password;

    return (
        <main className="auth-page" aria-label="Login">
            <section className="auth-card" aria-labelledby="login-title">
                <h1 id="login-title" className="auth-title">Login</h1>

                {errorMsg && (
                    <div className="auth-alert" role="alert">
                        {errorMsg}
                    </div>
                )}

                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    <div className="field">
                        <label htmlFor="email">E-Mail</label>
                        <input
                            id="email"
                            type="email"
                            autoComplete="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            aria-invalid={Boolean(errorMsg)}
                        />
                    </div>

                    <div className="field">
                        <label htmlFor="password">Passwort</label>
                        <div className="pwd-wrap">
                            <input
                                id="password"
                                type={showPwd ? "text" : "password"}
                                autoComplete="current-password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                aria-invalid={Boolean(errorMsg)}
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
                    </div>

                    <button className="auth-btn" type="submit" disabled={isDisabled}>
                        {submitting ? <span className="spinner" aria-hidden="true" /> : null}
                        {submitting ? "Wird eingeloggt…" : "Login"}
                    </button>
                </form>

                <p className="auth-meta">
                    Noch kein Konto?{" "}
                    <Link to="/register" className="auth-link">Jetzt registrieren</Link>
                </p>
            </section>
        </main>
    );
}

export default Login;