package com.lohika.morning.ml.spark.driver.service.lyrics.pipeline;

import static com.lohika.morning.ml.spark.distributed.library.function.map.lyrics.Column.*;

import com.lohika.morning.ml.spark.distributed.library.function.map.lyrics.Column;
import com.lohika.morning.ml.spark.driver.service.MLService;
import com.lohika.morning.ml.spark.driver.service.lyrics.Genre;
import com.lohika.morning.ml.spark.driver.service.lyrics.GenrePrediction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.linalg.DenseVector;
import org.apache.spark.ml.tuning.CrossValidatorModel;
import org.apache.spark.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class CommonLyricsPipeline implements LyricsPipeline {

    @Autowired
    protected SparkSession sparkSession;

    @Autowired
    private MLService mlService;

    @Value("${lyrics.training.set.directory.path}")
    private String lyricsTrainingSetDirectoryPath;

    @Value("${lyrics.model.directory.path}")
    private String lyricsModelDirectoryPath;

    @Override
    public GenrePrediction predict(final String unknownLyrics) {
        String lyrics[] = unknownLyrics.split("\\r?\\n");
        Dataset<String> lyricsDataset = sparkSession.createDataset(Arrays.asList(lyrics),
                Encoders.STRING());

        Dataset<Row> unknownLyricsDataset = lyricsDataset
                .withColumn(LABEL.getName(), functions.lit(Genre.UNKNOWN.getValue()))
                .withColumn(ID.getName(), functions.lit("unknown.txt"));

        CrossValidatorModel model = mlService.loadCrossValidationModel(getModelDirectory());
        getModelStatistics(model);

        PipelineModel bestModel = (PipelineModel) model.bestModel();

        Dataset<Row> predictionsDataset = bestModel.transform(unknownLyricsDataset);
        Row predictionRow = predictionsDataset.first();

        System.out.println("\n------------------------------------------------");
        final Double prediction = predictionRow.getAs("prediction");
        System.out.println("Prediction: " + Double.toString(prediction));

        List<Map<String, Object>> listOfProbabilities = new ArrayList<>();

        if (Arrays.asList(predictionsDataset.columns()).contains("probability")) {
            final DenseVector probability = predictionRow.getAs("probability");
            System.out.println("Probability: " + probability);
            System.out.println("------------------------------------------------\n");

            for (double i = 0; i < probability.size(); i++) {
                Map<String, Object> dictionary = new HashMap<>();
                dictionary.put("genre", getGenre(i).getName());
                dictionary.put("value", probability.apply((int) i));
                listOfProbabilities.add(dictionary);
            }


            return new GenrePrediction(getGenre(prediction).getName(), listOfProbabilities);
        }

        System.out.println("------------------------------------------------\n");
        return new GenrePrediction(getGenre(prediction).getName());
    }

    Dataset<Row> readLyrics() {
        SplitCSVUsingTextFile(lyricsTrainingSetDirectoryPath);
        Dataset input = readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.POP)
                .union(readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.COUNTRY))
                .union(readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.BLUES))
                .union(readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.HIP_HOP))
                .union(readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.JAZZ))
                .union(readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.REGGAE))
                .union(readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.ROCK))
                .union(readLyricsForGenre(lyricsTrainingSetDirectoryPath, Genre.GOSPEL));
        // Reduce the input amount of partition minimal amount (spark.default.parallelism OR 2, whatever is less)
        input = input.coalesce(sparkSession.sparkContext().defaultMinPartitions()).cache();
        // Force caching.
        input.count();

        return input;
    }

    private void SplitCSVUsingTextFile(String inputDirectory) {
        // Read the CSV as raw text
        Dataset<Row> df = sparkSession.read().option("header", "true")   // First row as header
                .option("inferSchema", "true")  // Automatically detect data types
                .csv(Paths.get(inputDirectory).resolve("Merged_dataset.csv").toString());

        // Column to split by (change "category" to your column name)
        String splitColumn = "genre";

        // Get unique values in the column
        df.select(splitColumn).distinct().collectAsList().forEach(row -> {
            String value = row.getString(0);
            Dataset<Row> filteredDF = df.filter(functions.col(splitColumn).equalTo(value))
                    .withColumnRenamed("lyrics", Column.VALUE.getName());

            // Save each partition as a separate CSV file
            String outputPath = inputDirectory + value + "/";  // Output directory
            filteredDF.write()
                    .option("header", "true")
                    .mode(SaveMode.Ignore)
                    .csv(outputPath);

            System.out.println("Saved: " + outputPath);
        });
    }

    private Dataset<Row> readLyricsForGenre(String inputDirectory, Genre genre) {
        Dataset<Row> lyrics = readLyrics(inputDirectory, genre.getName().toLowerCase() + "/*");
        Dataset<Row> labeledLyrics = lyrics.withColumn(LABEL.getName(), functions.lit(genre.getValue()));

        System.out.println(genre.name() + " music sentences = " + lyrics.count());

        return labeledLyrics;
    }

    private Dataset<Row> readLyrics(String inputDirectory, String path) {
        /*Dataset<String> rawLyrics = sparkSession.read().textFile(Paths.get(inputDirectory).resolve(path).toString());
        rawLyrics = rawLyrics.filter(rawLyrics.col(VALUE.getName()).notEqual(""));
        rawLyrics = rawLyrics.filter(rawLyrics.col(VALUE.getName()).contains(" "));

        // Add source filename column as a unique id.
        Dataset<Row> lyrics = rawLyrics.withColumn(ID.getName(), functions.input_file_name());*/

        return sparkSession.read().option("header", "true").option("inferSchema", "true")
                .csv(Paths.get(inputDirectory).resolve(path).toString());
    }

    private Genre getGenre(Double value) {
        for (Genre genre : Genre.values()) {
            if (genre.getValue().equals(value)) {
                return genre;
            }
        }

        return Genre.UNKNOWN;
    }

    @Override
    public Map<String, Object> getModelStatistics(CrossValidatorModel model) {
        Map<String, Object> modelStatistics = new HashMap<>();

        Arrays.sort(model.avgMetrics());
        modelStatistics.put("Best model metrics", model.avgMetrics()[model.avgMetrics().length - 1]);

        return modelStatistics;
    }

    void printModelStatistics(Map<String, Object> modelStatistics) {
        System.out.println("\n------------------------------------------------");
        System.out.println("Model statistics:");
        System.out.println(modelStatistics);
        System.out.println("------------------------------------------------\n");
    }

    void saveModel(CrossValidatorModel model, String modelOutputDirectory) {
        this.mlService.saveModel(model, modelOutputDirectory);
    }

    void saveModel(PipelineModel model, String modelOutputDirectory) {
        this.mlService.saveModel(model, modelOutputDirectory);
    }

    public void setLyricsTrainingSetDirectoryPath(String lyricsTrainingSetDirectoryPath) {
        this.lyricsTrainingSetDirectoryPath = lyricsTrainingSetDirectoryPath;
    }

    public void setLyricsModelDirectoryPath(String lyricsModelDirectoryPath) {
        this.lyricsModelDirectoryPath = lyricsModelDirectoryPath;
    }

    protected abstract String getModelDirectory();

    String getLyricsModelDirectoryPath() {
        return lyricsModelDirectoryPath;
    }
}
