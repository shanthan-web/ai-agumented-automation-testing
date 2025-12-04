package com.shanthan.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around OpenAI chat completions API.
 */
@Component
public class OpenAiClient {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String openAiApiKey;
    private final String baseUrl;
    private final String model;

    public OpenAiClient(
            @Value("${openai.apiKey:${OPENAI_API_KEY:}}") String openAiApiKey,
            @Value("${openai.baseUrl:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.model:gpt-4.1-mini}") String model) {
        this.openAiApiKey = openAiApiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String generateAnalysis(String systemPrompt, String userContent) {
        try {
            if (openAiApiKey == null || openAiApiKey.isBlank()) {
                return "OpenAI API key is not configured. Set OPENAI_API_KEY or openai.apiKey.";
            }

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", model);
            payload.put("temperature", 0.2);
            payload.put("max_tokens", 1000);

            ArrayNode messages = payload.putArray("messages");
            messages.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", systemPrompt));
            messages.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", userContent));

            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MEDIA_TYPE_JSON);

            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + openAiApiKey)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "LLM call failed with HTTP " + response.code();
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode choices = rootNode.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
                return "LLM returned no choices";
            }
        } catch (Exception e) {
            return "Exception during LLM call: " + e.getMessage();
        }
    }
}
