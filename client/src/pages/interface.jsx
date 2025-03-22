import { useState } from "react";
import { PieChart, Pie, Cell, Tooltip, Legend } from "recharts";

const API_URL = "/lyrics/predict";
const COLORS = ["#FF5733", "#33FF57", "#3357FF", "#FFD700", "#FF33A1", "#33FFF3", "#9933FF", "#FF8C00"];

export default function SongGenrePredictor() {
    const [lyrics, setLyrics] = useState("");
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async () => {
        if (!lyrics.trim()) return;
        setLoading(true);
        setChartData([]); // Hide previous chart data
        try {
            const response = await fetch(API_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ lyrics }),
            });
            const data = await response.json();

            setChartData(data.probabilities.map((item, index) => ({
                name: item.genre,
                value: item.value * 100,
                fill: COLORS[index % COLORS.length],
            })));
        } catch (error) {
            console.error("Error fetching data:", error);
        }
        setLoading(false);
    };

    return (
        <div className="h-screen w-screen flex flex-col items-center justify-center bg-gray-50 p-6">
            {/* Title */}
            <h1 className="text-4xl font-bold text-gray-800 mb-6">Song Genre Predictor</h1>

            {/* Lyrics Input */}
            <textarea
                className="w-3/4 md:w-1/2 p-3 border rounded-lg shadow-sm resize-none focus:ring-2 focus:ring-blue-500"
                rows="5"
                placeholder="Enter song lyrics..."
                value={lyrics}
                onChange={(e) => setLyrics(e.target.value)}
            />

            {/* Predict Button */}
            <button
                className="mt-4 px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg shadow-md hover:bg-blue-700 transition disabled:opacity-50"
                onClick={handleSubmit}
                disabled={loading}
            >
                {loading ? "Analyzing..." : "Predict Genre"}
            </button>

            {/* Centered Loading Spinner */}
            {loading && (
                <div className="flex justify-center items-center mt-6">
                    <div className="w-16 h-16 border-8 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                </div>
            )}

            {/* Pie Chart (Hidden When Loading) */}
            {!loading && chartData.length > 0 && (
                <PieChart width={500} height={500} className="mt-8">
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
                    <Tooltip />
                    <Legend />
                </PieChart>
            )}
        </div>
    );
}
