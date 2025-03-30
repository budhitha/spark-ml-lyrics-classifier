// src/App.jsx
import { Routes, Route, Link } from "react-router-dom";
import SongGenrePredictor from "./pages/interface.jsx";

function App() {
    return (
        <div>
            <nav>
                <Link to="/">SongGenrePredictor</Link>
            </nav>

            <Routes>
                <Route path="/" element={<SongGenrePredictor />} />
            </Routes>
        </div>
    );
}

export default App;
