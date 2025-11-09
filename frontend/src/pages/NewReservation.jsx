import ReservationForm from "../components/ReservationForm";
import '../styles/newreservation.css';
import { useNavigate } from "react-router-dom";

/**
 * Seite zum Erstellen einer neuen Tischreservierung.
 *
 * <p>Diese Seite enthält ausschließlich das <code>ReservationForm</code>-Formular
 * und einen Zurück-Button, der den Benutzer zum Konto-Bereich
 * (<code>/myaccount</code>) navigiert.</p>
 *
 * <ul>
 *   <li>Rendert das Formular in einem zentralen Container.</li>
 *   <li>Verwendet <code>useNavigate()</code> für die Navigation.</li>
 *   <li><code>setReservation</code> wird hier als leere Funktion übergeben,
 *       da nach dem Anlegen keine sofortige Anzeige erfolgt.</li>
 * </ul>
 *
 * @component
 * @returns {JSX.Element}
 */
function NewReservation() {
    const navigate = useNavigate();

    return (
        <main className="newreservation-page">
            <section className="newreservation-container">
                <div className="newreservation-header">
                    <button
                        onClick={() => navigate("/myaccount")}
                        className="back-button"
                        type="button"
                    >
                        ← Zurück
                    </button>
                    <h1>Neue Reservierung</h1>
                </div>

                <ReservationForm setReservation={() => {}} />
            </section>
        </main>
    );
}

export default NewReservation;