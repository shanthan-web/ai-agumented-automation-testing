package com.shanthan.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.IOException;

/**
 * Thin wrapper around OpenAI's Chat Completions API.
 * - Reads config from application.yml:
 *     openai.apiKey
 *     openai.baseUrl
 *     openai.model
 * - Sends system + user messages
 * - Returns the assistant's message content as a String
 * - On 429 / error, returns a stubbed fallback JSON so the rest of the
 *   framework continues to work for demo purposes.
 */
@Component
public class OpenAiClient {

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public OpenAiClient(
            @Value("${openai.apiKey:}") String apiKey,
            @Value("${openai.baseUrl:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.model:gpt-4.1-mini}") String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @PostConstruct
    public void init() {
        System.out.println(
                "DEBUG >>> OpenAiClient initialized. " +
                        "Model=" + model +
                        ", baseUrl=" + baseUrl +
                        ", apiKey length=" + (apiKey == null ? "null" : apiKey.length())
        );
    }

    /**
     * Calls OpenAI Chat Completions with the given prompts and returns the
     * assistant message content as a String.
     * IMPORTANT:
     *  - We assume the prompt instructs the model to respond with JSON.
     *  - On 429 / quota / other errors, we return a stub JSON so the project
     *    is still can be demoed even without real OpenAI responses.
     */
    public String generateAnalysis(String systemPrompt, String userPrompt) {
        // If API key is missing, don't even try; return stubbed response
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("DEBUG >>> OPENAI_API_KEY missing, returning stubbed analysis.");
            return stubbedResponse("AI key not configured. This is a stubbed fallback response.",
                    "Configure OPENAI_API_KEY to enable real AI triage.");
        }

        try {
            // Build the chat/completions request body
            JsonNode bodyJson = buildRequestBody(systemPrompt, userPrompt);
            String bodyString = mapper.writeValueAsString(bodyJson);

            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(bodyString, JSON))
                    .build();

            System.out.println("DEBUG >>> Calling OpenAI chat.completions with model: " + model);

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("DEBUG >>> OpenAI HTTP status: " + response.code());
                System.out.println("DEBUG >>> OpenAI response body length: " + responseBody.length());

                if (!response.isSuccessful()) {
                    System.out.println("DEBUG >>> OpenAI error body: " + responseBody);

                    // If quota / rate limit / auth errors -> return stubbed JSON
                    if (response.code() == 429 || response.code() == 401 || response.code() == 403) {
                        return stubbedResponse(
                                "AI triage is temporarily unavailable (OpenAI quota / rate limit / auth issue). This is a stubbed fallback response.",
                                "Review this failure manually. Check OpenAI billing/usage or key if you want live AI triage."
                        );
                    }

                    return stubbedResponse(
                            "AI triage failed with an upstream error. This is a stubbed fallback response.",
                            "Review the failure manually and check AI service logs."
                    );
                }

                // Parse the assistant's message content:
                // {
                //   "choices": [
                //     {
                //       "message": {
                //         "role": "assistant",
                //         "content": "{ ...JSON we asked for... }"
                //       }
                //     }
                //   ]
                // }
                return extractAssistantContent(responseBody);
            }
        } catch (IOException e) {
            System.out.println("DEBUG >>> OpenAI call failed: " + e.getMessage());
            return stubbedResponse(
                    "AI triage failed due to connection/exception. This is a stubbed fallback response.",
                    "Check network / API key configuration."
            );
        } catch (Exception e) {
            System.out.println("DEBUG >>> OpenAI unexpected error: " + e.getMessage());
            return stubbedResponse(
                    "AI triage crashed while parsing the LLM response. This is a stubbed fallback response.",
                    "Check AI service logs and response structure."
            );
        }
    }

    private JsonNode buildRequestBody(String systemPrompt, String userPrompt) {
        var root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);

        var messages = root.putArray("messages");

        var sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);

        var user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        return root;
    }

    /**
     * Extracts the assistant's message.content from the OpenAI response JSON.
     * If parsing fails, we just return the raw response.
     */
    private String extractAssistantContent(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                System.out.println("DEBUG >>> OpenAI response has no choices array; returning raw body.");
                return responseBody;
            }

            JsonNode first = choices.get(0);
            JsonNode message = first.path("message");
            String content = message.path("content").asText();

            if (content == null || content.isBlank()) {
                System.out.println("DEBUG >>> OpenAI assistant content is empty; returning raw body.");
                return responseBody;
            }

            return content;
        } catch (Exception e) {
            System.out.println("DEBUG >>> Failed to parse OpenAI response: " + e.getMessage());
            return responseBody;
        }
    }

    /**
     * Stubbed JSON that matches the expected structure of the model output.
     * This keeps the rest of the pipeline working even when OpenAI is unavailable.
     */
    private String stubbedResponse(String rootCause, String nextSteps) {
        return """
        {
          "failureType": "UNKNOWN",
          "rootCauseSummary": "%s",
          "recommendedNextSteps": "%s",
          "severityScore": 2,
          "jiraSummaryTemplate": "Investigate test failure (AI triage unavailable)",
          "aiConfidence": 0.0
        }
        """.formatted(escapeForJson(rootCause), escapeForJson(nextSteps));
    }

    private String escapeForJson(String text) {
        if (text == null) return "";
        // very basic escaping for quotes/newlines
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

