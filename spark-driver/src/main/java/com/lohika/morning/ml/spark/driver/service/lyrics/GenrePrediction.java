package com.lohika.morning.ml.spark.driver.service.lyrics;

import java.util.List;
import java.util.Map;

public class GenrePrediction {

    private String genre;
    private List<Map<String, Object>> probabilities;

    public GenrePrediction(String genre, List<Map<String, Object>> probabilities) {
        this.genre = genre;
        this.probabilities = probabilities;
    }

    public GenrePrediction(String genre) {
        this.genre = genre;
    }

    public String getGenre() {
        return genre;
    }

    public List<Map<String, Object>> getProbabilities() {
        return probabilities;
    }
}
