package com.lld;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class AnthropicClient implements LlmClient {
    private final String apiKey, model;
    private final Duration timeout;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public AnthropicClient(String apiKey, String model, Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) throw new Errors.Evaluator("ANTHROPIC_API_KEY not set");
        this.apiKey = apiKey; this.model = model; this.timeout = timeout;
    }

    @SuppressWarnings("unchecked")
    public String complete(String system, String prompt) {
        String body = Json.write(Map.of("model", model, "max_tokens", 1000, "system", system,
                "messages", List.of(Map.of("role", "user", "content", prompt))));
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages")).timeout(timeout)
                .header("x-api-key", apiKey).header("anthropic-version", "2023-06-01").header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new Errors.Evaluator("LLM call failed: HTTP " + res.statusCode());
            StringBuilder text = new StringBuilder();
            for (Object b : (List<Object>) ((Map<String, Object>) Json.parse(res.body())).get("content"))
                text.append(((Map<String, Object>) b).getOrDefault("text", ""));
            return text.toString();
        } catch (IOException e) {
            throw new Errors.Evaluator("LLM call failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Errors.Evaluator("LLM call interrupted", e);
        } catch (RuntimeException e) {
            if (e instanceof Errors.PracticeException) throw e;
            throw new Errors.Evaluator("LLM response unreadable: " + e.getMessage(), e);
        }
    }
}
