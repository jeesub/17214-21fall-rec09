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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {
    private static final int NUM_REQUESTS = 30;

    public static <T> void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        List<CompletableFuture<HttpResponse<String>>> allFutures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_REQUESTS);
        HttpClient client = HttpClient.newBuilder().executor(executorService).build();
        String bodyStr = new String(Files.readAllBytes(Paths.get("src/main/resources/request-body.json")));
        String key = "";
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://api.clarifai.com/v2/models/bd367be194cf45149e75f01d59f77ba7/outputs"))
            .header("Authorization", "Key " + key)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
            .build();

        // Run requests asynchronously
        Instant start = Instant.now();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            allFutures.add(responseFuture);
        }
        List<HttpResponse<String>> result = allFutures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

//        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            HttpResponse<String> response = result.get(i);
//            System.out.println(response.body());
        }
        System.out.println("Total time async (ms): " + Duration.between(start, Instant.now()).toMillis());

        // Run requests again synchronously
        start = Instant.now();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            System.out.println(response.body());
        }
        System.out.println("Total time sync (ms): " + Duration.between(start, Instant.now()).toMillis());
    }
}
