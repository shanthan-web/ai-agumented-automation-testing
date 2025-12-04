package com.shanthan.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthan.ai.client.OpenAiClient;
import com.shanthan.ai.model.FailureAnalysisRequest;
import com.shanthan.ai.model.FailureAnalysisResponse;
import com.shanthan.ai.model.FailureType;
import com.shanthan.ai.model.SimilarFailure;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class FailureAnalysisService {

    private final SimilarityStore similarityStore;
    private final OpenAiClient openAiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public FailureAnalysisService(SimilarityStore similarityStore, OpenAiClient openAiClient) {
        this.similarityStore = similarityStore;
        this.openAiClient = openAiClient;
    }

    public FailureAnalysisResponse analyzeFailure(FailureAnalysisRequest request) {

        List<SimilarFailure> similarFailures = similarityStore.findSimilar(
                request.getFailureMessage(), request.getStackTrace());


        String systemPrompt = """
                   You are an experienced SDET expert and test architect.
                   you receive test failure details, similar past failures and few historical failure details.
                   and you are job is to:
                   1. Classify the failure into one of these buckets ONLY:
                         - LOCATOR_ISSUE
                         - ENVIRONMENT
                         - BACKEND_5XX
                         - AUTHENTICATION
                         - TEST_DATA
                         - UNKNOWN
                   2. summarize the most likely root cause.
                   3. recommend the concrete next steps to the owning team.
                   4. Propose a certain jira summary in one line
                   4. return severity score from 1(low) to 5(high).
                   5. Return AI confidence score between 0.0 and 1.0.
                
                Very important rule:
                    - if you are not reasonably sure about the classification, root cause, next steps, jira summary or severity score,
                      classify it as UNKNOWN, write "Insufficient data for analysis" as root cause summary,
                      "Investigate logs and environment" as next steps,
                      "Investigate failure in test <test name>" as jira summary and
                    - DO NOT invent Jira IDs, ticket numbers, or system names that are not in the input..
                    - respond only in json format as per the below schema:
                   {
                       "failureType": "LOCATOR_ISSUE | ENVIRONMENT | BACKEND_5XX | AUTHENTICATION | TEST_DATA | UNKNOWN",
                       "rootCauseSummary": "string",
                       "recommendedNextSteps": "string",
                       "severityScore": int,              // 1–5
                       "jiraSummaryTemplate": "string"
                       "aiConfidence": float          // 0.0 - 1.0
                   }
                
                
                """;
        StringBuilder userBuilder = new StringBuilder();
        userBuilder.append("Suite: ").append(nullSafe(request.getSuiteName())).append("\n");
        userBuilder.append("Test: ").append(nullSafe(request.getTestName())).append("\n");
        userBuilder.append("Feature: ").append(nullSafe(request.getFeature())).append("\n");
        userBuilder.append("Environment: ").append(nullSafe(request.getEnvironment())).append("\n\n");
        userBuilder.append("Failure message:\n").append(request.getFailureMessage()).append("\n\n");
        userBuilder.append("Stacktrace:\n").append(request.getStackTrace()).append("\n\n");
        userBuilder.append("Raw log snippet:\n").append(request.getRawLogSnippet()).append("\n\n");
        userBuilder.append("Existing similar failures:\n");
        for (SimilarFailure s : similarFailures) {
            userBuilder.append("- [").append(s.getId()).append("] ")
                    .append(s.getShortDescription())
                    .append(" | root cause: ").append(s.getSuspectedRootCause())
                    .append(" | link: ").append(s.getLink())
                    .append("\n");
        }

        String llmRaw = openAiClient.generateAnalysis(systemPrompt, userBuilder.toString());

        FailureAnalysisResponse response = new FailureAnalysisResponse();
        response.setSimilarFailures(similarFailures);

        try {
            JsonNode json = mapper.readTree(llmRaw);
            //Parse fields safely

            String failureTypeStr = json.path("failureType").asText("UNKNOWN");
            int severity = json.path("severityScore").asInt(3);
            double aiconfidence = (json.path("aiConfidence").asDouble(0.5));

            // 2. Clamp severity to [1..5]
            if (severity < 1 || severity > 5) {
                severity = 3;
            }

            // 3. Map failureType to enum; fallback to UNKNOWN if invalid
            FailureType parsedType;
            try {
                parsedType = FailureType.valueOf(failureTypeStr);
            } catch (IllegalArgumentException ex) {
                parsedType = FailureType.UNKNOWN;
            }

            response.setFailureType(parsedType);
            response.setSeverityScore(severity);
            response.setAiConfidence(aiconfidence);
            response.setRootCauseSummary(json.path("rootCauseSummary").asText("Insufficient data for analysis"));
            response.setRecommendedNextSteps(json.path("recommendedNextSteps").asText("Investigate logs and environment"));
            response.setJiraSummaryTemplate(json.path("jiraSummaryTemplate").asText("Investigate failure in test " + request.getTestName()));

        } catch (Exception e) {
            // If parsing fails, just dump the raw content into rootCauseSummary.
            response.setFailureType(FailureType.UNKNOWN);
            response.setSeverityScore(2);
            response.setAiConfidence(0.0);
            response.setRootCauseSummary("LLM response could not be parsed. Raw output:\n" + llmRaw);
            response.setRecommendedNextSteps("Review the failure manually. AI analysis unavailable.");
            response.setJiraSummaryTemplate("Investigate failure in test " + request.getTestName());
        }

        // Apply simple rule-based sanity check on the stacktrace as a guardrail
        FailureType heuristicType = inferFailureTypeFromStacktrace(request.getStackTrace());

        if (heuristicType != FailureType.UNKNOWN) {
            FailureType modelType = response.getFailureType();

            if (modelType == null || modelType == FailureType.UNKNOWN) {
                // If the model is unsure but heuristics see something obvious, use the heuristic type
                response.setFailureType(heuristicType);
                response.setRuleBasedOverrideApplied(true);
                if (response.getAiConfidence() < 0.5) {
                    response.setAiConfidence(0.5);
                }
            } else if (modelType != heuristicType) {
                // Model disagrees with an obvious pattern → downgrade confidence and optionally override
                response.setAiConfidence(Math.min(response.getAiConfidence(), 0.4));
                response.setRuleBasedOverrideApplied(true);
            }
        }

        return response;

    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Very lightweight heuristic classification based on obvious signals
     * in the stacktrace. This is not meant to be perfect; just a safety net.
     */
    private FailureType inferFailureTypeFromStacktrace(String stacktrace) {
        if (stacktrace == null) return FailureType.UNKNOWN;

        String s = stacktrace.toLowerCase();

        if (s.contains("nosuchelementexception") || s.contains("staleelementreferenceexception")) {
            return FailureType.LOCATOR_ISSUE;
        }
        if (s.contains("connectexception") || s.contains("connection refused") || s.contains("timeout")) {
            return FailureType.ENVIRONMENT;
        }
        if (s.contains("500") || s.contains("internal server error")) {
            return FailureType.BACKEND_5XX;
        }
        if (s.contains("unauthorized") || s.contains("401") || s.contains("403")) {
            return FailureType.AUTHENTICATION;
        }

        return FailureType.UNKNOWN;
    }


}
