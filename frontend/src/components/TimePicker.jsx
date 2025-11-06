// src/components/TimePicker.jsx
import { useEffect, useRef, useState } from "react";

const pad = (n) => (n < 10 ? `0${n}` : `${n}`);

export default function TimePicker({
                                       hour,
                                       minute,
                                       minHour = 12,
                                       maxHour = 21,       // 22 wyłączone (lokal zamyka się o 22)
                                       stepMinutes = 30,
                                       onChange,
                                   }) {
    const [h, setH] = useState(String(hour ?? minHour));
    const [m, setM] = useState(String(minute ?? "00"));
    const debRef = useRef(null);
    const lastSent = useRef(`${hour}:${minute}`);

    // trzymamy lokalny stan i wysyłamy do rodzica z debounce, żeby nie spamować fetchem
    useEffect(() => {
        clearTimeout(debRef.current);
        debRef.current = setTimeout(() => {
            const key = `${h}:${m}`;
            if (key !== lastSent.current) {
                lastSent.current = key;
                onChange?.({ hour: h, minute: m });
            }
        }, 300);
        return () => clearTimeout(debRef.current);
    }, [h, m, onChange]);

    // gdy propsy przychodzą z zewnątrz (np. reset formularza)
    useEffect(() => {
        if (hour != null) setH(String(hour));
        if (minute != null) setM(String(minute));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [hour, minute]);

    const incHour = () => setH((prev) => {
        const n = Math.min(+prev + 1, maxHour);
        return String(n);
    });
    const decHour = () => setH((prev) => {
        const n = Math.max(+prev - 1, minHour);
        return String(n);
    });

    const setMinute = (val) => setM(val);

    return (
        <div className="tp-root">
            <div className="tp-field">
                <button type="button" className="tp-btn" onClick={decHour} aria-label="Hour down">‹</button>
                <div className="tp-value" aria-label="Hour">{pad(+h)}</div>
                <button type="button" className="tp-btn" onClick={incHour} aria-label="Hour up">›</button>
            </div>

            <div className="tp-sep">:</div>

            <div className="tp-field">
                <select
                    className="tp-select"
                    value={m}
                    onChange={(e) => setMinute(e.target.value)}
                    aria-label="Minute"
                >
                    {stepMinutes === 30 ? (
                        <>
                            <option value="00">00</option>
                            <option value="30">30</option>
                        </>
                    ) : (
                        Array.from({ length: 60 / stepMinutes }, (_, i) => {
                            const mm = pad(i * stepMinutes);
                            return <option key={mm} value={mm}>{mm}</option>;
                        })
                    )}
                </select>
            </div>

            <style>
                {`
        .tp-root { display:flex; align-items:center; gap:8px; }
        .tp-field { display:flex; align-items:center; gap:6px; }
        .tp-btn {
          width:28px; height:28px; line-height:28px;
          border:1px solid #ccc; border-radius:6px;
          background:#fafafa; cursor:pointer; font-size:14px; padding:0;
        }
        .tp-btn:active { transform: translateY(1px); }
        .tp-value {
          min-width:36px; text-align:center; font-variant-numeric: tabular-nums;
          padding:4px 6px; border:1px solid #ddd; border-radius:6px; background:#fff;
        }
        .tp-sep { opacity:0.7; }
        .tp-select {
          height:30px; border:1px solid #ddd; border-radius:6px; padding:0 6px;
          background:#fff; font-size:14px; appearance:auto;
        }
        `}
            </style>
        </div>
    );
}