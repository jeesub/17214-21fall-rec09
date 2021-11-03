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
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {
    private static final int NUM_REQUESTS = 300;
    private static final int MAX_CONCURRENT = 10;
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
        /**
         * this uses a semaphore that only at most MAX_CONCURRENT requests are processed in parallel
         * uses a semaphore to count how many requests are running
         *
         * this works even with a single thread in the worker pool
         *
         * this takes longer, but does not overwhelm the server (i.e. takes at least 300/5 * .5 seconds)
         */
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://feature.isri.cmu.edu:3003"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        client = HttpClient.newBuilder().executor(executorService).build();
        Semaphore parallelRequestCounter = new Semaphore(MAX_CONCURRENT);
        Instant start = Instant.now();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            parallelRequestCounter.acquire();
            CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            responseFuture.thenAccept(System.out::println);
            responseFuture.thenRun(parallelRequestCounter::release);
            futures.add(responseFuture);
        }
        List<HttpResponse<String>> result = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println("Total time async (ms): " + Duration.between(start, Instant.now()).toMillis());
    }

    private static void runMultipleAsynchronous_AllAtOnce() throws IOException, InterruptedException {
        /**
         * this is not a good solution as it will send all requests immediately.
         * they are processed asynchrounously, but all issued at the same time
         *
         * this responds very fast (<1s for 300 requests) and the answers show up pretty much all at the same time
         * showing that they are requests at the same time
         */
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://feature.isri.cmu.edu:3003"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        List<CompletableFuture<HttpResponse<String>>> allFutures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT);
        client = HttpClient.newBuilder().executor(executorService).build();
        Instant start = Instant.now();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            responseFuture.thenAccept(System.out::println);
            allFutures.add(responseFuture);
        }
        List<HttpResponse<String>> result = allFutures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println("Total time async (ms): " + Duration.between(start, Instant.now()).toMillis());
    }

    private static Queue<HttpRequest> requestQueue = new LinkedList<>();
    private static int concurrentRequestsCounter = 0;

    private static void runMultipleRecursive() throws IOException, InterruptedException {
        /**
         * this one queues requests and then starts a few of them, ending the main thread
         *
         * the remaining ones are queued each time one a request completes.
         *
         * this does similar batching as the semaphore solution above, but it does not block the main thread.
         * this is recognizable easily because the "main thread is done" message appears at the beginning, not the end
         *
         * note, this code probably isn't properly thread-safe.
         */

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://feature.isri.cmu.edu:3003"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            requestQueue.add(request);
        }
        startRequestsRecursive();
    }

    private synchronized static void startRequestsRecursive() {
        while (concurrentRequestsCounter < MAX_CONCURRENT && !requestQueue.isEmpty()) {
            concurrentRequestsCounter++;
            HttpRequest request = requestQueue.poll();
            CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            responseFuture.thenAccept((r) -> {
                concurrentRequestsCounter--;
                System.out.println(r);
                startRequestsRecursive();
            });
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Task 1
//        runWebAPIRequest();
        // Task 2
        runMultipleSynchronous();
        runMultipleAsynchronous();
    }
}
