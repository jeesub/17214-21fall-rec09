package edu.cmu.cs.cs214.rec09;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {

    public static <T> void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        HttpClient client = HttpClient.newHttpClient();
        String bodyStr = new String(Files.readAllBytes(Paths.get("src/main/resources/request-body.json")));
        String key = "";
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://api.clarifai.com/v2/models/bd367be194cf45149e75f01d59f77ba7/outputs"))
            .header("Authorization", "Key " + key)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
            .build();

        CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response = responseFuture.get();
        System.out.println(response.body());
    }
}
