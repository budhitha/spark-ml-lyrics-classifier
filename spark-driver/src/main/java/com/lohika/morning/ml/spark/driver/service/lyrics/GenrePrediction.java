package com.lohika.morning.ml.spark.driver.service.lyrics;

public class GenrePrediction {

    private String genre;
    private Double countryProbability;
    private Double popProbability;
    private Double unknownProbability;

    public GenrePrediction(String genre, Double popProbability, Double countryProbability, Double unknownProbability) {
        this.genre = genre;
        this.countryProbability = countryProbability;
        this.popProbability = popProbability;
        this.unknownProbability = unknownProbability;
    }

    public GenrePrediction(String genre) {
        this.genre = genre;
    }

    public String getGenre() {
        return genre;
    }

    public Double getCountryProbability() {
        return countryProbability;
    }

    public Double getPopProbability() {
        return popProbability;
    }

    public Double getUnknownProbability() {
        return unknownProbability;
    }
}
