import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/adminreservations.css";

function AdminReservations() {
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            setErrorMsg("");
            try {
                const res = await fetch("http://localhost:8080/api/reservations/all", {
                    credentials: "include",
                });

                if (!res.ok) {

                    const text = await res.text();
                    setErrorMsg(
                        text || (res.status === 401 || res.status === 403
                            ? "Brak uprawnień (zaloguj się jako admin)."
                            : `Błąd ${res.status}`)
                    );
                    setReservations([]);
                    return;
                }


                const data = await res.json();
                setReservations(Array.isArray(data) ? data : []);
            } catch (e) {
                console.error(e);
                setErrorMsg(e.message || "Nie udało się pobrać rezerwacji.");
                setReservations([]);
            } finally {
                setLoading(false);
            }
        };

        load();
    }, []);

    const formatDateTime = (start, end) => {
        const s = new Date(start);
        const e = new Date(end);
        const d = s.toLocaleDateString("de-DE", { day: "2-digit", month: "2-digit", year: "numeric" });
        const t1 = s.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" });
        const t2 = e.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" });
        return `${d} – ${t1} bis ${t2} Uhr`;
    };

    const deleteReservation = async (id) => {
        try {
            const res = await fetch(`http://localhost:8080/api/reservations/${id}`, {
                method: "DELETE",
                credentials: "include",
            });
            if (!res.ok) {
                const t = await res.text();
                throw new Error(t || `Löschen fehlgeschlagen: ${res.status}`);
            }
            setReservations(prev => prev.filter(r => r.id !== id));
        } catch (err) {
            console.error("Fehler beim Löschen der Reservierung:", err);
            setErrorMsg(err.message || "Błąd podczas usuwania rezerwacji.");
        }
    };

    return (
        <main className="admin-resv-page" aria-label="Alle Reservierungen">
            <section className="admin-resv-card">
                <div className="admin-resv-header">
                    <button onClick={() => navigate("/admin")} className="back-button" type="button">
                        ← Zurück
                    </button>
                    <h1 className="admin-resv-title">Alle Reservierungen</h1>
                </div>

                {loading ? (
                    <div className="skeleton-list" aria-busy="true" aria-live="polite">
                        <div className="skeleton-row" />
                        <div className="skeleton-row" />
                        <div className="skeleton-row" />
                    </div>
                ) : errorMsg ? (
                    <p className="empty-state">{errorMsg}</p>
                ) : reservations.length === 0 ? (
                    <p className="empty-state">Keine Reservierungen gefunden.</p>
                ) : (
                    <ul className="admin-reservation-list">
                        {reservations.map(res => (
                            <li key={res.id} className="res-row">
                                <div className="reservation-info">
                                    <div><strong>Benutzer:</strong> {res.fullName || res.username || "–"}</div>
                                    <div><strong>Tisch:</strong> {res.tableNumber ?? "?"}</div>
                                    <div><strong>Zeit:</strong> {formatDateTime(res.startTime, res.endTime)}</div>
                                </div>

                                <button
                                    type="button"
                                    className="delete-btn"
                                    onClick={() => deleteReservation(res.id)}
                                    aria-label={`Reservierung ${res.id} löschen`}
                                >
                                    Löschen
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </main>
    );
}

export default AdminReservations;