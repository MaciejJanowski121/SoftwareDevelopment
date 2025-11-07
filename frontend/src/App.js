import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

// Seiten-Komponenten
import Home from "./pages/Home";

import Reservations from "./pages/Reservations";
import Register from "./pages/Register";
import Login from "./pages/Login";
import MyAccount from "./pages/MyAccount";
import ChangePassword from "./components/ChangePassword";
import AdminPanel from "./pages/AdminPanel";
import NewReservation from "./pages/NewReservation";
import AdminReservations from "./pages/AdminReservations";

// Gemeinsame Komponenten
import Header from "./components/Header";
import Validation from "./components/Validation";
import IstLoggedCheck from "./components/IstLoggedCheck";

import "./styles/global.css";

function App() {
    return (
        <div className="app-container">
            <Router>

                {/* Navigationsleiste immer sichtbar */}
                <Header />

                {/* Routen für die SPA */}
                <Routes>
                    {/* Startseite */}
                    <Route path="/" element={<Home />} />

                    {/* Login & Registrierung – nur zugänglich, wenn nicht eingeloggt */}
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

                    {/* Geschützter Benutzerbereich */}
                    <Route
                        path="/myaccount"
                        element={
                            <Validation>
                                <MyAccount />
                            </Validation>
                        }
                    />
                    <Route
                        path="/changePassword"
                        element={<ChangePassword />}
                    />

                    {/* Admin-Bereich – nur mit Admin-Rechten sichtbar */}
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

                    {/* Reservierungsseiten – ebenfalls geschützt */}
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