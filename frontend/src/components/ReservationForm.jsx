
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

/**
 * Öffnungszeiten-/Form-Grenzen.
 * - MIN_MIN/MAX_MIN: erlaubte Dauer in Minuten
 * - OPENING_HOUR/LAST_START_HOUR: früheste/späteste Startzeit (Ende max. 22:00)
 */
const MIN_MIN = 30, MAX_MIN = 300, OPENING_HOUR = 12, LAST_START_HOUR = 21;

/** Links mit führender Null (z. B. 9 -> "09"). */
const pad = (n) => (n < 10 ? `0${n}` : `${n}`);

/** Wandelt ein Date in ISO_LOCAL_DATE_TIME (yyyy-MM-dd'T'HH:mm:ss) um. */
const toIso = (d) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;

/**
 * Erzeugt den Startzeitpunkt aus Datum (yyyy-MM-dd), Stunde und Minute.
 * @param {string} date ISO-Datum (yyyy-MM-dd)
 * @param {string|number} h Stunde
 * @param {string|number} m Minute
 * @returns {Date|null}
 */
const buildStart = (date, h, m) => (date ? new Date(`${date}T${pad(+h)}:${pad(+m)}:00`) : null);

/**
 * Berechnet die maximal zulässige Dauer (in Minuten) für einen gegebenen Start,
 * sodass das Ende nicht nach 22:00 Uhr liegt.
 * @param {Date|null} start
 * @returns {number}
 */
const maxMinutesForStart = (start) => {
    if (!start) return MAX_MIN;
    const end22 = new Date(start);
    end22.setHours(22, 0, 0, 0);
    return Math.max(0, Math.min(MAX_MIN, Math.floor((end22 - start) / 60000)));
};

/**
 * Debounce-Effect-Hook.
 *
 * Führt eine Funktion erst nach Ablauf von `ms` aus. Falls sich die Abhängigkeiten
 * vorher ändern oder die Komponente unmountet, wird abgebrochen (AbortController).
 *
 * @param {(signal: AbortSignal) => (void|Promise<void>)} fn
 * @param {any[]} deps
 * @param {number} [ms=300]
 */
function useDebouncedEffect(fn, deps, ms = 300) {
    const ctrlRef = useRef();
    useEffect(() => {
        const t = setTimeout(() => fn((ctrlRef.current = new AbortController()).signal), ms);
        return () => {
            clearTimeout(t);
            ctrlRef.current?.abort();
        };
    }, deps);
}

/**
 * Lädt verfügbare Tische für ein gegebenes Startdatum und eine Dauer.
 *
 * - Debounced-Abfrage (300ms), um unnötige Requests zu vermeiden.
 * - Respektiert Öffnungszeiten und "Ende bis 22:00"-Regel clientseitig.
 *
 * @param {string} API Basis-URL des Backends
 * @param {Date|null} startDateObj Startzeitpunkt
 * @param {number} minutes gewünschte Dauer in Minuten
 * @returns {Array<{id:number, tableNumber:number, numberOfSeats:number}>}
 */
function useAvailableTables(API, startDateObj, minutes) {
    const [data, setData] = useState([]);
    const key = useMemo(
        () => (startDateObj ? `${toIso(startDateObj)}|${minutes}` : ""),
        [startDateObj, minutes]
    );

    useDebouncedEffect(
        async (signal) => {
            if (!startDateObj || !minutes) return setData([]);
            const h = startDateObj.getHours();
            const mm = startDateObj.getMinutes();
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

/**
 * Formular zur Erstellung einer Tischreservierung.
 *
 * <p>Funktionen:</p>
 * <ul>
 *   <li>Ermittelt zulässige Start-/Endezeiten (Öffnung 12:00, letzte Startzeit 21:30, Ende bis 22:00).</li>
 *   <li>Lädt verfügbare Tische dynamisch (debounced), abhängig von Startzeit und Dauer.</li>
 *   <li>Validiert Eingaben clientseitig; zeigt konkrete Fehlermeldungen in <code>formError</code> an.</li>
 *   <li>Reagiert auf Konflikte aus dem Backend (ProblemDetail-Typen) mit sprechenden Meldungen.</li>
 * </ul>
 *
 * <p><strong>Barrierefreiheit/Testbarkeit:</strong> Labels sind mit <code>htmlFor</code> verknüpft;
 * ein Fehlertext trägt <code>data-cy="form-error"</code> für End-to-End-Tests.</p>
 *
 * @component
 * @param {{ setReservation: (list: any[]) => void }} props
 * @returns {JSX.Element}
 */
export default function ReservationForm({ setReservation }) {
    const navigate = useNavigate();
    const API = useMemo(() => process.env.REACT_APP_API_URL || "http://localhost:8080", []);

    const today = useMemo(() => {
        const d = new Date();
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    }, []);

    const [date, setDate] = useState("");
    const [hour, setHour] = useState("12");
    const [minute, setMinute] = useState("00");
    const [minutes, setMinutes] = useState(120);
    const [tableNumber, setTableNumber] = useState("");
    const [formError, setFormError] = useState("");

    useEffect(() => {
        const now = new Date();
        const next = { d: today, h: now.getHours(), m: now.getMinutes() < 30 ? "30" : "00" };
        if (next.m === "00" && now.getMinutes() >= 30) next.h++;
        if (next.h < OPENING_HOUR) next.h = OPENING_HOUR;
        if (next.h > LAST_START_HOUR || (next.h === LAST_START_HOUR && next.m > "30")) {
            const t = new Date();
            t.setDate(t.getDate() + 1);
            next.d = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`;
            next.h = OPENING_HOUR;
            next.m = "00";
        }
        setDate(next.d);
        setHour(String(next.h));
        setMinute(next.m);
    }, [today]);

    const start = buildStart(date, hour, minute);
    const maxAllowed = maxMinutesForStart(start);
    const endPreview = start ? new Date(start.getTime() + minutes * 60000) : null;

    const availableTables = useAvailableTables(API, start, minutes);

    useEffect(() => {
        if (minutes > maxAllowed) {
            const clamped = Math.max(MIN_MIN, maxAllowed);
            setMinutes(clamped);
            setFormError("");
        }
    }, [maxAllowed, minutes]);

    useEffect(() => {
        if (!tableNumber) return;
        if (!availableTables.some((t) => String(t.tableNumber) === String(tableNumber))) {
            setTableNumber("");
            setFormError("Der ausgewählte Tisch ist nicht mehr verfügbar.");
        }
    }, [availableTables, tableNumber]);

    /**
     * Submit-Handler: validiert clientseitig und sendet anschließend an das Backend.
     * @param {React.FormEvent} e
     * @returns {Promise<void>}
     */
    async function onSubmit(e) {
        e.preventDefault();
        const err = (m) => (setFormError(m), false);

        if (!start) return err("Bitte Startzeit auswählen.");
        const h = start.getHours();
        const mm = start.getMinutes();
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
                id="startDate"
                type="date"
                value={date}
                min={today}
                onChange={(e) => {
                    setDate(e.target.value);
                    setFormError("");
                }}
                required
            />

            <label>Uhrzeit</label>
            <div className="time-row">
                <select
                    id="hour"
                    value={hour}
                    onChange={(e) => {
                        setHour(e.target.value);
                        setFormError("");
                    }}
                >
                    {Array.from({ length: LAST_START_HOUR - OPENING_HOUR + 1 }, (_, i) => OPENING_HOUR + i).map((h) => (
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
                    }}
                >
                    <option value="00">00</option>
                    <option value="30">30</option>
                </select>
            </div>

            <label htmlFor="duration">Dauer</label>
            <select
                id="duration"
                value={minutes}
                required
                onChange={(e) => {
                    setMinutes(+e.target.value);
                    setFormError("");
                }}
            >
                {Array.from({ length: (MAX_MIN - MIN_MIN) / 30 + 1 }, (_, i) => MIN_MIN + i * 30).map((m) => (
                    <option key={m} value={m} disabled={m > maxAllowed}>
                        {m < 60 ? `${m} Min` : `${Math.floor(m / 60)} Std${m % 60 ? ` ${m % 60} Min` : ""}`}
                        {m > maxAllowed ? " (bis 22:00 Uhr)" : ""}
                    </option>
                ))}
            </select>

            <label htmlFor="tableNumber">Tisch</label>
            <select
                id="tableNumber"
                required
                value={tableNumber}
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

            {formError && (
                <p data-cy="form-error" style={{ color: "#c62828", marginTop: 10 }}>
                    {formError}
                </p>
            )}

            <button type="submit" disabled={!start || !tableNumber || maxAllowed < MIN_MIN} style={{ marginTop: 12 }}>
                Reservieren
            </button>
        </form>
    );
}