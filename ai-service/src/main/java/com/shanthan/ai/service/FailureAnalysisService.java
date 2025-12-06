package com.shanthan.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthan.ai.client.OpenAiClient;
import com.shanthan.ai.model.FailureAnalysisResponse;
import com.shanthan.ai.model.FailureEventPayload;
import com.shanthan.ai.model.FailureType;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class FailureAnalysisService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public FailureAnalysisService(SimilarityStore similarityStore, OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public FailureAnalysisResponse analyzeFailure(FailureEventPayload request) {

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request);

            String llmRaw = openAiClient.generateAnalysis(systemPrompt, userPrompt);

            System.out.println("DEBUG >>> LLM raw response: " + llmRaw);
            FailureAnalysisResponse response;

            try {
                response = mapper.readValue(llmRaw, FailureAnalysisResponse.class);
            } catch (Exception ex) {
                response = fallbackResponse(
                        "AI response could not be parsed. This is a fallback triage.",
                        "Review the failure manually and check AI service logs.");
            }
            // Ensure non-null similarFailures
            if (response.getSimilarFailures() == null) {
                response.setSimilarFailures(new ArrayList<>());
            }

            // Apply simple rule-based overrides (optional but nice)
            applyRuleOverrides(request, response);

            // Optionally seed similarity examples by type
            seedSimilarityIfEmpty(request, response);

            return response;

        } catch (Exception e) {
            System.out.println("DEBUG >>> FailureAnalysisService.analyze error: " + e.getMessage());
            return fallbackResponse(
                    "AI triage failed due to an exception in the analysis service.",
                    "Review logs and validate the AI pipeline configuration.");
        }
    }

    @NotNull
    private static String buildSystemPrompt() {
        return """
            You are an expert QA/SDET assistant that triages both UI and API test failures.
            
            You will always be given:
            - testType: "UI" or "API"
            - failureMessage and stackTrace
            - For UI tests: Selenium-style exceptions and locators (id/xpath/css)
            - For API tests: httpMethod, endpoint, statusCode, requestBody, responseBody
            
            Your job:
            1. Classify the failure into a concrete failureType.
            2. Explain the most likely root cause in 1–3 sentences.
            3. Suggest 2–4 concrete next steps for an engineer.
            4. Suggest a one-line Jira summary.
            5. Optionally return similarFailures ONLY IF the caller has provided real historical data.
               In this project, assume there is NO real history unless explicitly provided.
            
            ### Allowed failureType values
            
            For UI tests (testType="UI"):
            - "LOCATOR_ISSUE"             (element not found, wrong selector, stale element)
            - "WAITING_SYNC_ISSUE"        (timing, missing waits, page not ready)
            - "BROWSER_ENVIRONMENT"       (driver mismatch, browser config, capabilities)
            - "TEST_DATA_ISSUE"           (data not created/loaded, wrong credentials, missing seed)
            - "FLAKY_TEST"                (intermittent, timing-dependent, infra flakiness)
            - "OTHER"
            
            For API tests (testType="API"):
            - "CLIENT_REQUEST_ISSUE"      (bad payload, wrong expectation, contract mismatch on client side)
            - "SERVER_BUG"                (5xx when the request is valid)
            - "ENVIRONMENT_ISSUE"         (DNS, timeouts, auth config, env mismatch, upstream service down)
            - "CONTRACT_MISMATCH"         (OpenAPI/Swagger vs actual response shape/status)
            - "AUTHENTICATION_AUTHORIZATION"
            - "RATE_LIMITING_THROTTLING"
            - "DATA_DEPENDENCY"           (missing setup data, ordering issues)
            - "FLAKY_TEST"
            - "OTHER"
            
            ### IMPORTANT RULES
            
            - Use UI failureTypes ONLY when testType="UI".
            - Use API failureTypes ONLY when testType="API".
            - Do NOT invent JIRA IDs or ticket links.
            - For this project, similarFailures MUST follow these rules:
              - If no historical failures are provided in the input, return `"similarFailures": []`.
              - Do NOT fabricate or hallucinate similar failures, IDs or URLs.
              - If historical failure snippets ARE provided, you may summarize them in similarFailures.
            
            ### Output format
            
            You MUST respond with STRICT JSON, NO extra text:
            
            {
              "failureType": "<one of the allowed values>",
              "rootCauseSummary": "<short paragraph>",
              "recommendedNextSteps": "<bulleted or numbered steps in a single string>",
              "severityScore": <integer 1-5>,
              "jiraSummaryTemplate": "<one-line summary>",
              "similarFailures": [
                {
                  "id": "<optional id or empty>",
                  "shortDescription": "<short text>",
                  "suspectedRootCause": "<short text>",
                  "link": "<optional link or empty>"
                }
              ],
              "aiConfidence": <0.0-1.0>,
              "ruleBasedOverrideApplied": false
            }
            """;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String snippet(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "... [truncated]";
    }

    private String buildUserPrompt(FailureEventPayload request){
        return """
            testType: %s
            testName: %s
            suiteName: %s
            feature: %s
            environment: %s
            
            failureMessage:
            %s
            
            stackTrace:
            %s
            
            httpMethod: %s
            endpoint: %s
            statusCode: %s
            
            requestBody (may be truncated):
            %s
            
            responseBody (may be truncated):
            %s
            """.formatted(
                      nullSafe(request.getTestType()),
                nullSafe(request.getTestName()),
                nullSafe(request.getSuiteName()),
                nullSafe(request.getFeature()),
                nullSafe(request.getEnvironment()),
                nullSafe(request.getFailureMessage()),
                nullSafe(request.getStackTrace()),
                nullSafe(request.getHttpMethod()),
                nullSafe(request.getEndpoint()),
                request.getStatusCode() == null ? "" : request.getStatusCode().toString(),
                snippet(request.getRequestBody(), 2000),
                snippet(request.getResponseBody(), 2000)
        );
    }

    private FailureAnalysisResponse fallbackResponse(String rootCause,
                                                     String nextSteps) {
        FailureAnalysisResponse r = new FailureAnalysisResponse();
        r.setFailureType(FailureType.UNKNOWN);
        r.setRootCauseSummary(rootCause);
        r.setRecommendedNextSteps(nextSteps);
        r.setSeverityScore(2);
        r.setJiraSummaryTemplate("Investigate test failure (AI triage fallback)");
        r.setSimilarFailures(new ArrayList<>());
        r.setAiConfidence(0.0);
        r.setRuleBasedOverrideApplied(false);
        return r;
    }
    /**
     * Simple guardrails to correct obvious classifications.
     * You already saw this for locator issues; you can expand later.
     */
    private void applyRuleOverrides(FailureEventPayload request, FailureAnalysisResponse response) {
        String stack = nullSafe(request.getStackTrace());
        String msg = nullSafe(request.getFailureMessage());
        String type = nullSafe(request.getTestType());

        // Example: Selenium NoSuchElement -> LOCATOR_ISSUE
        if ("UI".equalsIgnoreCase(type) &&
                (stack.contains("NoSuchElementException") || msg.contains("no such element"))) {
            response.setFailureType(FailureType.LOCATOR_ISSUE);
            response.setRuleBasedOverrideApplied(true);
            response.setAiConfidence(Math.max(response.getAiConfidence(), 0.9));
        }

        // You can add more patterns for API (e.g., 4xx vs 5xx, auth errors, etc.)
    }

    /**
     * For now, we either:
     *  - leave similarFailures empty (recommended), OR
     *  - seed a couple of type-aware examples for demo purposes.
     *
     * Right now, this seeds nothing to avoid mixing UI/API.
     * If you want demo seeds, you can add them here based on request.getTestType().
     */
    private void seedSimilarityIfEmpty(FailureEventPayload request, FailureAnalysisResponse response) {
        if (response.getSimilarFailures() != null && !response.getSimilarFailures().isEmpty()) {
            return;
        }

        // If you want NO seeds (clean, non-hallucinated), just leave it empty:
        response.setSimilarFailures(new ArrayList<>());

        // If later you want seeds, uncomment and customize:

        /*
        List<SimilarFailure> seeds = new ArrayList<>();
        if ("API".equalsIgnoreCase(request.getTestType())) {
            seeds.add(new SimilarFailure(
                    "API-101",
                    "User creation API returns 404 in staging",
                    "Missing route or misconfigured base URL in staging environment",
                    ""
            ));
            seeds.add(new SimilarFailure(
                    "API-202",
                    "Order service returns 500 on large payloads",
                    "Uncaught backend exception when validating request body",
                    ""
            ));
        } else {
            seeds.add(new SimilarFailure(
                    "UI-101",
                    "Login button click fails on slow/staging environment",
                    "Element not interactable due to missing explicit wait",
                    ""
            ));
            seeds.add(new SimilarFailure(
                    "UI-202",
                    "Product thumbnail not visible on category page",
                    "Wrong CSS selector after frontend refactor",
                    ""
            ));
        }
        response.setSimilarFailures(seeds);
        */
    }
}
