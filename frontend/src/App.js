import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Home from "./pages/Home";
import Reservations from "./pages/Reservations";
import Register from "./pages/Register";
import Login from "./pages/Login";
import MyAccount from "./pages/MyAccount";
import ChangePassword from "./components/ChangePassword";
import AdminPanel from "./pages/AdminPanel";
import NewReservation from "./pages/NewReservation";
import AdminReservations from "./pages/AdminReservations";

import Header from "./components/Header";
import Validation from "./components/Validation";
import IstLoggedCheck from "./components/IstLoggedCheck";

import "./styles/global.css";

/**
 * Hauptkomponente der React Single-Page-Application (SPA).
 *
 * <p>Definiert die komplette Routing-Struktur der Anwendung
 * mithilfe von <code>react-router-dom</code>. Der Header ist
 * dauerhaft sichtbar; geschützte Routen sind mit der Komponente
 * <code>Validation</code> versehen, die den Login-Status prüft.
 * Seiten wie Login oder Register werden nur angezeigt, wenn
 * der Benutzer noch nicht eingeloggt ist (<code>IstLoggedCheck</code>).</p>
 *
 * <ul>
 *   <li><strong>Öffentliche Seiten:</strong> Home, Login, Register</li>
 *   <li><strong>Benutzerbereich:</strong> MyAccount, ChangePassword, Reservations</li>
 *   <li><strong>Adminbereich:</strong> AdminPanel, AdminReservations</li>
 * </ul>
 *
 * @component
 * @returns {JSX.Element} Hauptcontainer der App mit Routing.
 */
function App() {
    return (
        <div className="app-container">
            <Router>
                <Header />

                <Routes>
                    <Route path="/" element={<Home />} />

                    <Route
                        path="/login"
                        element={
                            <IstLoggedCheck>
                                <Login />
                            </IstLoggedCheck>
                        }
                    />
                    <Route
                        path="/register"
                        element={
                            <IstLoggedCheck>
                                <Register />
                            </IstLoggedCheck>
                        }
                    />

                    <Route
                        path="/myaccount"
                        element={
                            <Validation>
                                <MyAccount />
                            </Validation>
                        }
                    />
                    <Route path="/changePassword" element={<ChangePassword />} />

                    <Route
                        path="/admin"
                        element={
                            <Validation>
                                <AdminPanel />
                            </Validation>
                        }
                    />
                    <Route
                        path="/admin/reservations"
                        element={
                            <Validation>
                                <AdminReservations />
                            </Validation>
                        }
                    />

                    <Route
                        path="/reservations/new"
                        element={
                            <Validation>
                                <NewReservation />
                            </Validation>
                        }
                    />
                    <Route
                        path="/reservations/my"
                        element={
                            <Validation>
                                <Reservations />
                            </Validation>
                        }
                    />
                </Routes>
            </Router>
        </div>
    );
}

export default App;