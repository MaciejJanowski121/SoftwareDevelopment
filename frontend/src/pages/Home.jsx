import '../styles/home.css';
import '../styles/global.css';

/**
 * Startseite der Anwendung.
 *
 * <p>Zeigt Begrüßung, Highlights und Restaurantinformationen
 * in einem zweispaltigen Layout.</p>
 *
 * @component
 * @returns {JSX.Element} Startseite.
 */

function Home() {
    return (
        <main
            className="home split-hero swapped"
            role="main"
            aria-label="Startseite Restaurant"
        >



            <section className="split-left">
                <p className="home-kicker">Seit 1998 – Familiengeführt</p>

                <h1 className="home-title">
                    Willkommen in unserem Restaurant!
                </h1>

                <p className="home-subtitle">
                    Reservieren Sie einen Tisch und genießen Sie köstliches Essen!
                </p>

                <ul className="home-highlights" aria-label="Highlights">
                    <li>Frische Zutaten</li>
                    <li>Veggie & Vegan</li>
                    <li>Hausgemachte Desserts</li>
                    <li>Lokale Weinkarte</li>
                </ul>
            </section>


            <aside className="split-right card" aria-label="Informationen">
                <div className="info-row">
                    <div>
                        <h2>Öffnungszeiten</h2>
                        <p>Mo–Fr: 12:00–22:00</p>
                        <p>Sa–So: 12:00–22:00</p>
                    </div>
                    <div>
                        <h2>Adresse</h2>
                        <p>Beispielstraße 12</p>
                        <p>86150 Augsburg</p>
                    </div>
                    <div>
                        <h2>Kontakt</h2>
                        <p>Tel: +49 821 123456</p>
                        <p>mail@restaurant.de</p>
                    </div>
                </div>
            </aside>
        </main>
    );
}

export default Home;