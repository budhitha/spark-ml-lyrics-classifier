import { useState } from "react";
import { PieChart, Pie, Cell, Tooltip, Legend } from "recharts";

const API_URL = "/lyrics/predict";
const COLORS = ["#FF5733", "#33FF57", "#3357FF", "#FFD700", "#FF33A1", "#33FFF3", "#9933FF", "#FF8C00"];

export default function SongGenrePredictor() {
    const [lyrics, setLyrics] = useState("");
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedGenre, setSelectedGenre] = useState(null); // New state for selected genre

    const handleSubmit = async () => {
        if (!lyrics.trim()) return;
        setLoading(true);
        setChartData([]); // Hide previous chart data
        setSelectedGenre(null); // Reset selected genre
        try {
            const response = await fetch(API_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ lyrics }),
            });
            const data = await response.json();

            if (!data.probabilities || !Array.isArray(data.probabilities)) {
                throw new Error("Invalid response from the server");
            }

            // Find the genre with the highest probability
            setSelectedGenre(data.genre); // Set selected genre

            // Prepare data for the pie chart
            setChartData(data.probabilities.map((item, index) => ({
                name: item.genre,
                value: item.value * 100,
                fill: COLORS[index % COLORS.length],
            })));
        } catch (error) {
            console.error("Error fetching data:", error);
            alert("Failed to fetch genre predictions. Please try again.");
        }
        setLoading(false);
    };

    const handleReset = () => {
        setLyrics("");
        setChartData([]);
        setSelectedGenre(null); // Reset selected genre
    };

    return (
        <div  style={{ display: "flex", height: "100vh", padding: "20px", backgroundColor: "#f9fafb" }}>
            <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
                <h1 style={{ fontSize: "32px", fontWeight: "bold", color: "#333" }}>Song Genre Predictor</h1>

                {/* Lyrics Input */}
                <textarea
                    style={{ width: "80%", padding: "10px", border: "1px solid #ccc", borderRadius: "5px", resize: "none" }}
                    rows="5"
                    placeholder="Enter song lyrics..."
                    value={lyrics}
                    onChange={(e) => setLyrics(e.target.value)}
                />

                {/* Predict and Reset Buttons */}
                <div style={{ marginTop: "10px" }}>
                    <button
                        style={{ padding: "10px 20px", backgroundColor: "blue", color: "white", borderRadius: "5px", marginRight: "10px" }}
                        onClick={handleSubmit}
                        disabled={loading}
                    >
                        {loading ? "Analyzing..." : "Predict Genre"}
                    </button>
                    <button
                        style={{ padding: "10px 20px", backgroundColor: "red", color: "white", borderRadius: "5px" }}
                        onClick={handleReset}
                    >
                        Reset
                    </button>
                </div>
            </div>

            <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", position: "relative" }}>
                {/* Centered Loading Spinner */}
                {loading && (
                    <div
                        style={{
                            display: "flex",
                            justifyContent: "center",
                            alignItems: "center",
                            marginTop: "1.5rem",
                        }}
                        role="status"
                        aria-live="polite"
                        aria-label="Loading content"
                    >
                        <div
                            style={{
                                width: "4rem",
                                height: "4rem",
                                border: "8px solid #3b82f6", // blue-500
                                borderTop: "8px solid transparent",
                                borderRadius: "50%",
                                animation: "spin 1s linear infinite",
                            }}
                        ></div>
                        <style>
                            {`
                                @keyframes spin {
                                    from { transform: rotate(0deg); }
                                    to { transform: rotate(360deg); }
                                }
                            `}
                        </style>
                    </div>
                )}

                {/* Display Selected Genre */}
                {!loading && selectedGenre && (
                    <div  style={{ position: "absolute", top: "20px", fontSize: "22px", fontWeight: "bold", color: "#333" }}>
                        Predicted Genre: <span style={{ color: "blue" }}>{selectedGenre}</span>
                    </div>
                )}

                {/* Pie Chart (Hidden When Loading) */}
                <div style={{ width: "100%", display: "flex", justifyContent: "center", alignItems: "center" }}>
                    {!loading && chartData.length > 0 && (
                        <PieChart width={550} height={500}>
                            <Pie
                                data={chartData}
                                cx="50%"
                                cy="50%"
                                outerRadius={150}
                                dataKey="value"
                                label={({ name, percent }) =>
                                    `${name} ${(percent * 100).toFixed(0)}%`
                                }
                            >
                                {chartData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.fill} />
                                ))}
                            </Pie>
                            <Tooltip
                                formatter={(value, name, props) => [
                                    `${value.toFixed(2)}%`,
                                    name,
                                ]}
                            />
                            <Legend
                                layout="horizontal"
                                align="center"
                                verticalAlign="bottom"
                                wrapperStyle={{ paddingLeft: "20px" }}
                            />
                        </PieChart>
                    )}
                </div>
            </div>
        </div>
    );
}