// src/components/ReservationForm.jsx
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

const MIN_MIN = 30, MAX_MIN = 300, OPENING_HOUR = 12, LAST_START_HOUR = 21;
const pad = (n) => (n < 10 ? `0${n}` : `${n}`);
const toIso = (d) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
const buildStart = (date, h, m) => (date ? new Date(`${date}T${pad(+h)}:${pad(+m)}:00`) : null);
const maxMinutesForStart = (start) => {
    if (!start) return MAX_MIN;
    const end22 = new Date(start); end22.setHours(22, 0, 0, 0);
    return Math.max(0, Math.min(MAX_MIN, Math.floor((end22 - start) / 60000)));
};

function useDebouncedEffect(fn, deps, ms = 300) {
    const ctrlRef = useRef();
    useEffect(() => {
        const t = setTimeout(() => fn((ctrlRef.current = new AbortController()).signal), ms);
        return () => { clearTimeout(t); ctrlRef.current?.abort(); };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, deps);
}

function useAvailableTables(API, startDateObj, minutes) {
    const [data, setData] = useState([]);
    const key = useMemo(
        () => (startDateObj ? `${toIso(startDateObj)}|${minutes}` : ""),
        [startDateObj, minutes]
    );
    useDebouncedEffect(
        async (signal) => {
            if (!startDateObj || !minutes) return setData([]);
            const h = startDateObj.getHours(), mm = startDateObj.getMinutes();
            if (h < OPENING_HOUR || h > LAST_START_HOUR || (h === LAST_START_HOUR && mm > 30)) return setData([]);
            if (maxMinutesForStart(startDateObj) < MIN_MIN) return setData([]);
            const res = await fetch(
                `${API}/api/reservations/available?start=${encodeURIComponent(toIso(startDateObj))}&minutes=${minutes}`,
                { credentials: "include", signal }
            );
            setData(res.ok ? await res.json() : []);
        },
        [API, key],
        300
    );
    return data;
}

export default function ReservationForm({ setReservation }) {
    const navigate = useNavigate();
    const API = useMemo(() => process.env.REACT_APP_API_URL || "http://localhost:8080", []);
    const today = useMemo(() => {
        const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    }, []);

    // form state
    const [date, setDate] = useState("");
    const [hour, setHour] = useState("12");
    const [minute, setMinute] = useState("00");
    const [minutes, setMinutes] = useState(120);
    const [tableNumber, setTableNumber] = useState("");
    const [formError, setFormError] = useState("");

    // init default date/time
    useEffect(() => {
        const now = new Date(), next = { d: today, h: now.getHours(), m: now.getMinutes() < 30 ? "30" : "00" };
        if (next.m === "00" && now.getMinutes() >= 30) next.h++;
        if (next.h < OPENING_HOUR) next.h = OPENING_HOUR;
        if (next.h > LAST_START_HOUR || (next.h === LAST_START_HOUR && next.m > "30")) {
            const t = new Date(); t.setDate(t.getDate() + 1);
            next.d = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`;
            next.h = OPENING_HOUR; next.m = "00";
        }
        setDate(next.d); setHour(String(next.h)); setMinute(next.m);
    }, [today]);

    const start = buildStart(date, hour, minute);
    const maxAllowed = maxMinutesForStart(start);
    const endPreview = start ? new Date(start.getTime() + minutes * 60000) : null;

    const availableTables = useAvailableTables(API, start, minutes);

    useEffect(() => {
        if (!tableNumber) return;
        if (!availableTables.some((t) => String(t.tableNumber) === String(tableNumber))) {
            setTableNumber(""); setFormError("Der ausgewählte Tisch ist nicht mehr verfügbar.");
        }
    }, [availableTables, tableNumber]);

    async function onSubmit(e) {
        e.preventDefault();
        const err = (m) => (setFormError(m), false);
        if (!start) return err("Bitte Startzeit auswählen.");
        const h = start.getHours(), mm = start.getMinutes();
        if (h < OPENING_HOUR || h > LAST_START_HOUR || (h === LAST_START_HOUR && mm > 30))
            return err("Reservierungen sind nur zwischen 12:00 und 21:30 (Ende bis 22:00) möglich.");
        if (start < new Date()) return err("Reservierungen in der Vergangenheit sind nicht möglich.");
        if (!tableNumber) return err("Bitte Tisch auswählen.");
        if (minutes < MIN_MIN || minutes > MAX_MIN || minutes > maxAllowed)
            return err(`Reservierungen müssen zwischen ${MIN_MIN} und ${Math.max(MIN_MIN, maxAllowed)} Minuten liegen.`);

        try {
            const end = new Date(start.getTime() + minutes * 60000);
            const res = await fetch(`${API}/api/reservations`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ tableNumber: +tableNumber, startTime: toIso(start), endTime: toIso(end) }),
            });

            if (res.status === 409) {
                let msg = "Reservierungskonflikt.";
                try {
                    const j = await res.json();
                    const type = String(j?.type || "");
                    if (type.includes("user-has-reservation")) msg = "Du hast bereits eine aktive Reservierung.";
                    else if (type.includes("table-already-reserved")) msg = "Dieser Tisch ist in dem gewählten Zeitraum bereits reserviert.";
                    else msg = j?.detail || j?.title || msg;
                } catch {}
                return setFormError(msg);
            }
            if (!res.ok) throw new Error(await res.text());

            const data = await res.json();
            setReservation([data]);
            navigate("/reservations/my");
        } catch (e) {
            setFormError(`Reservierung fehlgeschlagen: ${e.message || e}`);
        }
    }

    return (
        <form onSubmit={onSubmit} className="reservation-form" noValidate>
            <label htmlFor="startDate">Datum</label>
            <input
                id="startDate" type="date" value={date} min={today}
                onChange={(e) => { setDate(e.target.value); setFormError(""); }}
                required
            />

            <label>Uhrzeit</label>
            <div className="time-row">
                <select id="hour" value={hour} onChange={(e) => { setHour(e.target.value); setFormError(""); }}>
                    {Array.from({ length: LAST_START_HOUR - OPENING_HOUR + 1 }, (_, i) => OPENING_HOUR + i)
                        .map((h) => <option key={h} value={String(h)}>{pad(h)}</option>)}
                </select>
                <select id="minute" value={minute} onChange={(e) => { setMinute(e.target.value); setFormError(""); }}>
                    <option value="00">00</option><option value="30">30</option>
                </select>
            </div>

            <label htmlFor="duration">Dauer</label>
            <select
                id="duration" value={minutes} required
                onChange={(e) => { setMinutes(+e.target.value); setFormError(""); }}
            >
                {Array.from({ length: (MAX_MIN - MIN_MIN) / 30 + 1 }, (_, i) => MIN_MIN + i * 30)
                    .map((m) => (
                        <option key={m} value={m} disabled={m > maxAllowed}>
                            {m < 60 ? `${m} Min` : `${Math.floor(m / 60)} Std${m % 60 ? ` ${m % 60} Min` : ""}`}
                            {m > maxAllowed ? " (bis 22:00 Uhr)" : ""}
                        </option>
                    ))}
            </select>

            <label htmlFor="tableNumber">Tisch</label>
            <select
                id="tableNumber" required value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                disabled={maxAllowed < MIN_MIN}
            >
                <option value="">Tisch auswählen…</option>
                {availableTables.map((t) => (
                    <option key={t.id} value={t.tableNumber}>
                        Tisch {t.tableNumber} ({t.numberOfSeats} Pers.)
                    </option>
                ))}
            </select>

            {start && (
                <small style={{ display: "block", marginTop: 6, color: "#555" }}>
                    Ende: {new Date(start.getTime() + minutes * 60000).toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })} (spätestens 22:00 Uhr)
                </small>
            )}

            {formError && <p data-cy="form-error" style={{ color: "#c62828", marginTop: 10 }}>{formError}</p>}

            <button type="submit" disabled={!start || !tableNumber || maxAllowed < MIN_MIN} style={{ marginTop: 12 }}>
                Reservieren
            </button>
        </form>
    );
}