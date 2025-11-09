import { useState, useEffect, useMemo, useCallback } from "react";
import "../styles/reservations.css";
import { useNavigate } from "react-router-dom";

/**
 * Seite „Meine Reservierung“ für eingeloggte Nutzer.
 *
 * <p>Lädt die aktuelle Benutzer-Reservierung von
 * <code>GET /api/reservations/userReservations</code> (JWT-Cookie) und zeigt
 * sie in einer Karte an. Unterstützt manuelles Aktualisieren sowie Löschen
 * über <code>DELETE /api/reservations/{id}</code>.</p>
 *
 * <ul>
 *   <li>Rückfall auf leeren Zustand, wenn 204 (keine Reservierung).</li>
 *   <li>Bei 401 kurzer Hinweis und Weiterleitung zum Login.</li>
 *   <li>Fehler werden als Textmeldung oberhalb der Liste angezeigt.</li>
 * </ul>
 *
 * @component
 * @returns {JSX.Element}
 */
function Reservations() {
    const [reservation, setReservation] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState("");
    const navigate = useNavigate();

    /** Basis-URL des Backends (ENV-Override möglich). */
    const API = useMemo(
        () => process.env.REACT_APP_API_URL || "http://localhost:8080",
        []
    );

    /**
     * Lädt die aktuelle Reservierung des angemeldeten Benutzers.
     * @param {AbortSignal} [signal] optionales Abbruchsignal
     */
    const loadReservation = useCallback(
        async (signal) => {
            setLoading(true);
            setErrorMsg("");

            try {
                const res = await fetch(`${API}/api/reservations/userReservations`, {
                    credentials: "include",
                    signal,
                });

                const status = res.status;

                if (status === 401) {
                    setReservation(null);
                    setErrorMsg("Nie jesteś zalogowany.");
                    setTimeout(() => navigate("/login"), 300);
                    return;
                }

                if (status === 204) {
                    setReservation(null);
                    return;
                }

                if (res.ok) {
                    const raw = await res.text();
                    if (!raw) {
                        setReservation(null);
                        return;
                    }
                    let data;
                    try {
                        data = JSON.parse(raw);
                    } catch {
                        throw new Error(raw || "Odpowiedź nie jest JSON-em.");
                    }
                    setReservation(data && data.id ? data : null);
                    return;
                }

                const raw = await res.text();
                throw new Error(raw || `HTTP-Fehler: ${status}`);
            } catch (err) {
                if (err.name !== "AbortError") {
                    setErrorMsg(err.message || "Błąd podczas ładowania rezerwacji.");
                    setReservation(null);
                }
            } finally {
                setLoading(false);
            }
        },
        [API, navigate]
    );

    useEffect(() => {
        const ac = new AbortController();
        loadReservation(ac.signal);
        return () => ac.abort();
    }, [loadReservation]);

    /**
     * Löscht die Reservierung des Benutzers.
     * @param {number} id Reservierungs-ID
     */
    const deleteReservation = async (id) => {
        try {
            const res = await fetch(`${API}/api/reservations/${id}`, {
                method: "DELETE",
                credentials: "include",
            });
            if (!res.ok) {
                const t = await res.text();
                throw new Error(t || `Löschen fehlgeschlagen: ${res.status}`);
            }
            setReservation(null);
        } catch (err) {
            setErrorMsg(err.message || "Błąd podczas usuwania rezerwacji.");
        }
    };

    /** Formatiert ein ISO-Datum zu tt.mm.jjjj (de-DE). */
    const fmtDate = (iso) =>
        iso ? new Date(iso).toLocaleDateString("de-DE") : "Unbekannt";

    /** Formatiert Start/Ende zu „HH:MM – HH:MM“. */
    const fmtTime = (startIso, endIso) => {
        if (!startIso || !endIso) return "Unbekannt";
        const start = new Date(startIso);
        const end = new Date(endIso);
        const opts = { hour: "2-digit", minute: "2-digit" };
        return `${start.toLocaleTimeString("de-DE", opts)} – ${end.toLocaleTimeString("de-DE", opts)}`;
    };

    return (
        <main className="reservations-page" aria-label="Deine Reservierungen">
            <section className="reservations-container">
                <div className="reservations-header">
                    <button
                        onClick={() => navigate("/myaccount")}
                        className="reservations-back-button"
                        type="button"
                    >
                        ← Zurück
                    </button>
                    <h1>Deine Reservierung</h1>

                    <button
                        onClick={() => loadReservation()}
                        className="reservations-refresh"
                        type="button"
                        disabled={loading}
                        aria-label="Aktualisieren"
                    >
                        {loading ? "Aktualisiere…" : "Aktualisieren"}
                    </button>
                </div>

                {loading ? (
                    <div className="skeleton-list" aria-busy="true" aria-live="polite">
                        <div className="skeleton-row" />
                        <div className="skeleton-row" />
                    </div>
                ) : errorMsg ? (
                    <p className="error-text">{errorMsg}</p>
                ) : !reservation ? (
                    <p className="empty-state">Keine Reservierung vorhanden.</p>
                ) : (
                    <ul className="reservation-list">
                        <li key={reservation.id} className="res-card">
                            <div className="reservation-info">
                                <div>
                                    <strong>Benutzer:</strong>{" "}
                                    {reservation.fullName || reservation.username || "–"}
                                </div>
                                <div>
                                    <strong>Tischnummer:</strong>{" "}
                                    {reservation.tableNumber ?? "?"}
                                </div>
                                <div>
                                    <strong>Datum:</strong> {fmtDate(reservation.startTime)}
                                </div>
                                <div>
                                    <strong>Uhrzeit:</strong>{" "}
                                    {fmtTime(reservation.startTime, reservation.endTime)}
                                </div>
                            </div>
                            <button
                                className="delete-btn"
                                type="button"
                                onClick={() => deleteReservation(reservation.id)}
                                aria-label="Reservierung löschen"
                            >
                                Löschen
                            </button>
                        </li>
                    </ul>
                )}
            </section>
        </main>
    );
}

export default Reservations;