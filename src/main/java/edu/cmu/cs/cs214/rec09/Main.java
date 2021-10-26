package edu.cmu.cs.cs214.rec09;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {
    private static final int NUM_REQUESTS = 300;
    private static HttpClient client = HttpClient.newHttpClient();

    private static void runWebAPIRequest() throws IOException, InterruptedException {
        String bodyStr = new String(Files.readAllBytes(Paths.get("src/main/resources/request-body.json")));
        String key = "";
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://api.clarifai.com/v2/models/bd367be194cf45149e75f01d59f77ba7/outputs"))
            .header("Authorization", "Key " + key)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }

    private static void runMultipleSynchronous() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://feature.isri.cmu.edu:3003"))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        Instant start = Instant.now();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        }
        System.out.println("Total time sync (ms): " + Duration.between(start, Instant.now()).toMillis());
    }

    private static void runMultipleAsynchronous() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://feature.isri.cmu.edu:3003"))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        Instant start = Instant.now();
        // TODO your task 2 code here

        System.out.println("Total time async (ms): " + Duration.between(start, Instant.now()).toMillis());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Task 1
        runWebAPIRequest();
        // Task 2
//        runMultipleSynchronous();
//        runMultipleAsynchronous();
    }
}
