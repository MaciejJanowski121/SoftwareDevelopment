// src/components/ReservationForm.jsx
import { useState, useEffect, useMemo, useRef } from "react";
import { useNavigate } from "react-router-dom";

function ReservationForm({ setReservation }) {
    const [startDatePart, setStartDatePart] = useState("");
    const [hour, setHour] = useState("12");
    const [minute, setMinute] = useState("00");
    const [minutes, setMinutes] = useState(120); // 30..300
    const [tableNumber, setTableNumber] = useState("");
    const [availableTables, setAvailableTables] = useState([]);
    const [formError, setFormError] = useState("");

    const navigate = useNavigate();
    const API = useMemo(
        () => process.env.REACT_APP_API_URL || "http://localhost:8080",
        []
    );

    // reguły
    const MIN_MIN = 30;
    const MAX_MIN = 300;
    const OPENING_HOUR = 12;
    const LAST_SELECTABLE_HOUR = 21; // 22 wykluczone

    // utils
    const pad = (n) => (n < 10 ? "0" + n : "" + n);
    const todayDateStr = useMemo(() => {
        const d = new Date();
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    }, []);
    const toIsoWithSeconds = (date) =>
        `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
            date.getDate()
        )}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
            date.getSeconds()
        )}`;

    // hidden dla Cypressa (#startTime)
    const startTimePart = `${pad(Number(hour))}:${pad(Number(minute))}`;
    const buildStartDate = (date, hh, mm) =>
        date ? new Date(`${date}T${pad(Number(hh))}:${pad(Number(mm))}:00`) : null;

    const startDateObj = buildStartDate(startDatePart, hour, minute);

    // max tak, by koniec <= 22:00
    const maxMinutesForStart = (start) => {
        if (!start) return MAX_MIN;
        const latestEnd = new Date(start);
        latestEnd.setHours(22, 0, 0, 0);
        const diffMin = Math.floor((latestEnd - start) / 60000);
        if (diffMin <= 0) return 0;
        return Math.min(MAX_MIN, diffMin);
    };

    const maxAllowedForHint = maxMinutesForStart(startDateObj);
    const endPreview =
        startDateObj && minutes
            ? new Date(startDateObj.getTime() + minutes * 60000)
            : null;

    // domyśl: dziś + zaokrąglenie do 00/30; jeżeli <12 -> 12:00; jeżeli po 21:30 -> jutro 12:00
    useEffect(() => {
        const now = new Date();
        const todayStr = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(
            now.getDate()
        )}`;
        if (!startDatePart) setStartDatePart(todayStr);

        let h = now.getHours();
        let m = now.getMinutes() < 30 ? 30 : 0;
        if (m === 0 && now.getMinutes() >= 30) h++;

        // widełki 12..21 (22 nie wolno); po 21:30 skocz na jutro 12:00
        if (h < OPENING_HOUR) {
            h = OPENING_HOUR;
            m = 0;
        } else if (h > LAST_SELECTABLE_HOUR || (h === LAST_SELECTABLE_HOUR && m > 30)) {
            const t = new Date(now);
            t.setDate(now.getDate() + 1);
            const nextStr = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(
                t.getDate()
            )}`;
            setStartDatePart(nextStr);
            h = OPENING_HOUR;
            m = 0;
        }

        setHour(String(h));
        setMinute(pad(m));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // --- OGRANICZENIE SPAMU ZAPYTAŃ: debounce + abort + pamięć ostatniej krotki ---
    const lastQueryRef = useRef(""); // "startISO|minutes"
    useEffect(() => {
        setFormError("");

        // warunki brzegowe
        if (!startDateObj || !minutes) {
            setAvailableTables([]);
            return;
        }
        const h = startDateObj.getHours();
        const mm = startDateObj.getMinutes();
        // twarde widełki 12:00..21:30 — jeśli poza, nie pytamy backendu
        if (h < OPENING_HOUR || h > LAST_SELECTABLE_HOUR || (h === LAST_SELECTABLE_HOUR && mm > 30)) {
            setAvailableTables([]);
            return;
        }
        const maxAllowed = maxMinutesForStart(startDateObj);
        if (maxAllowed < MIN_MIN) {
            setAvailableTables([]);
            return;
        }

        const startISO = toIsoWithSeconds(startDateObj);
        const key = `${startISO}|${minutes}`;
        if (key === lastQueryRef.current) {
            // nic się nie zmieniło – nie odpalaj kolejnego fetcha
            return;
        }

        const ctrl = new AbortController();
        const t = setTimeout(async () => {
            try {
                const res = await fetch(
                    `${API}/api/reservations/available?start=${encodeURIComponent(
                        startISO
                    )}&minutes=${minutes}`,
                    { credentials: "include", signal: ctrl.signal }
                );
                if (!res.ok) throw new Error("Fehler beim Laden der verfügbaren Tische.");
                const data = await res.json();
                setAvailableTables(Array.isArray(data) ? data : []);
                lastQueryRef.current = key; // zapamiętaj powodzenie zapytania
            } catch (e) {
                if (e.name !== "AbortError") {
                    console.error(e);
                    setAvailableTables([]);
                }
            }
        }, 300); // debounce 300 ms

        return () => {
            clearTimeout(t);
            ctrl.abort();
        };
    }, [API, minutes, startDateObj]);

    // reset stolika jeśli zniknął
    useEffect(() => {
        if (!tableNumber) return;
        const stillAvail = availableTables.some(
            (t) => String(t.tableNumber) === String(tableNumber)
        );
        if (!stillAvail) setTableNumber("");
    }, [availableTables, tableNumber]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setFormError("");

        if (!startDateObj) return setFormError("Bitte Startzeit auswählen.");

        const h = startDateObj.getHours();
        const mm = startDateObj.getMinutes();
        // twarde widełki 12:00..21:30
        if (
            h < OPENING_HOUR ||
            h > LAST_SELECTABLE_HOUR ||
            (h === LAST_SELECTABLE_HOUR && mm > 30)
        ) {
            return setFormError(
                "Reservierungen sind nur zwischen 12:00 und 21:30 (Ende bis 22:00) möglich."
            );
        }

        if (startDateObj < new Date()) {
            return setFormError("Reservierungen in der Vergangenheit sind nicht möglich.");
        }

        if (!minutes || minutes < MIN_MIN || minutes > MAX_MIN) {
            return setFormError("Die Dauer muss zwischen 30 und 300 Minuten liegen.");
        }

        const maxAllowed = maxMinutesForStart(startDateObj);
        if (minutes > maxAllowed) {
            if (maxAllowed < MIN_MIN) {
                return setFormError(
                    "Die letzte Reservierung muss spätestens um 22:00 Uhr enden. Bitte frühere Zeit wählen."
                );
            }
            return setFormError(
                `Für diese Startzeit ist maximal ${maxAllowed} Minuten erlaubt (bis 22:00 Uhr).`
            );
        }

        if (!tableNumber) return setFormError("Bitte Tisch auswählen.");

        const end = new Date(startDateObj.getTime() + minutes * 60000);

        const payload = {
            tableNumber: Number(tableNumber),
            startTime: toIsoWithSeconds(startDateObj),
            endTime: toIsoWithSeconds(end),
        };

        try {
            const resp = await fetch(`${API}/api/reservations`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify(payload),
            });
            const txt = await resp.text();
            if (!resp.ok) throw new Error(txt || "Reservierung fehlgeschlagen.");

            const data = txt ? JSON.parse(txt) : null;
            setReservation([data]);
            navigate("/reservations/my");
        } catch (err) {
            console.error(err);
            setFormError(`Reservierung fehlgeschlagen: ${err.message}`);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="reservation-form" noValidate>
            {/* Data */}
            <label htmlFor="startDate" style={{ display: "block", marginBottom: 4 }}>
                Datum
            </label>
            <input
                id="startDate"
                type="date"
                value={startDatePart}
                min={todayDateStr}
                onChange={(e) => {
                    setStartDatePart(e.target.value);
                    setFormError("");
                    lastQueryRef.current = ""; // pozwól kolejnemu fetchowi się wykonać
                }}
                required
            />

            {/* Godzina + Minuty */}
            <label style={{ display: "block", margin: "12px 0 4px" }}>Uhrzeit</label>
            <div style={{ display: "flex", gap: 8 }}>
                <select
                    id="hour"
                    value={hour}
                    onChange={(e) => {
                        setHour(e.target.value);
                        setFormError("");
                        lastQueryRef.current = "";
                    }}
                    aria-label="Stunde"
                >
                    {Array.from(
                        { length: LAST_SELECTABLE_HOUR - OPENING_HOUR + 1 },
                        (_, i) => OPENING_HOUR + i
                    ).map((h) => (
                        <option key={h} value={String(h)}>
                            {pad(h)}
                        </option>
                    ))}
                </select>

                <select
                    id="minute"
                    value={minute}
                    onChange={(e) => {
                        setMinute(e.target.value);
                        setFormError("");
                        lastQueryRef.current = "";
                    }}
                    aria-label="Minute"
                >
                    <option value="00">00</option>
                    <option value="30">30</option>
                </select>
            </div>
            <small style={{ display: "block", marginTop: 4, color: "#555" }}>
                Start nur von <strong>12:00</strong> bis <strong>21:30</strong> (Ende spätestens 22:00).
            </small>

            {/* Hidden #startTime – dla Cypressa */}
            <input
                id="startTime"
                type="hidden"
                value={startDatePart ? `${startDatePart}T${startTimePart}` : ""}
                readOnly
            />

            {/* Dauer */}
            <label htmlFor="duration" style={{ display: "block", margin: "12px 0 4px" }}>
                Dauer
            </label>
            <select
                id="duration"
                value={minutes}
                onChange={(e) => {
                    setMinutes(Number(e.target.value));
                    setFormError("");
                    lastQueryRef.current = "";
                }}
                required
            >
                {Array.from({ length: (MAX_MIN - MIN_MIN) / 30 + 1 }, (_, i) => {
                    const m = MIN_MIN + i * 30;
                    const disabled = !!startDateObj && m > maxMinutesForStart(startDateObj);
                    const label =
                        m < 60 ? `${m} Min` : `${Math.floor(m / 60)} Std${m % 60 ? ` ${m % 60} Min` : ""}`;
                    return (
                        <option key={m} value={m} disabled={disabled}>
                            {label}
                            {disabled ? " (bis 22:00 Uhr)" : ""}
                        </option>
                    );
                })}
            </select>

            {/* Tische */}
            <label htmlFor="tableNumber" style={{ display: "block", margin: "12px 0 4px" }}>
                Tisch
            </label>
            <select
                id="tableNumber"
                value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                required
                disabled={maxAllowedForHint < MIN_MIN}
            >
                <option value="">Tisch auswählen…</option>
                {availableTables.map((t) => (
                    <option key={t.id} value={t.tableNumber}>
                        Tisch {t.tableNumber} ({t.numberOfSeats} Pers.)
                    </option>
                ))}
            </select>

            {/* Podgląd końca */}
            {endPreview && (
                <small style={{ display: "block", marginTop: 6, color: "#555" }}>
                    Ende:{" "}
                    {endPreview.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })}{" "}
                    (spätestens 22:00 Uhr)
                </small>
            )}

            {/* Błędy */}
            {formError && (
                <p data-cy="form-error" style={{ color: "#c62828", marginTop: 10 }}>
                    {formError}
                </p>
            )}

            <button type="submit" style={{ marginTop: 12 }} disabled={maxAllowedForHint < MIN_MIN}>
                Reservieren
            </button>
        </form>
    );
}

export default ReservationForm;