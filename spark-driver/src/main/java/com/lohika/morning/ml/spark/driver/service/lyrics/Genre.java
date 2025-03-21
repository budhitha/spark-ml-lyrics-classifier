package com.lohika.morning.ml.spark.driver.service.lyrics;

public enum Genre {

    POP("Pop <(^.^)/", 0D),

    COUNTRY("Country", 1D),

    UNKNOWN("Don\'t know :(", -1D);

    private final String name;
    private final Double value;

    Genre(final String name, final Double value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Double getValue() {
        return value;
    }

}
