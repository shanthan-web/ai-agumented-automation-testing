package com.shanthan.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthan.ai.client.OpenAiClient;
import com.shanthan.ai.model.FailureAnalysisRequest;
import com.shanthan.ai.model.FailureAnalysisResponse;
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
               1.classify the failure into high-level bukcet.
               2. guess the most likely root cause.
               3. recommend the next steps to the owning team to investigate and fix.
               4. assign severity score from 1(low) to 5(high).
               5. propose jira summary template for raising the defect.

               respond only in json format as per the below schema:
               {
                   "classifiedFailureType": "string",   // e.g., LOCATOR_ISSUE, ENVIRONMENT, BACKEND_500
                   "rootCauseSummary": "string",
                   "recommendedNextSteps": "string",
                   "severityScore": int,              // 1–5
                   "jiraSummaryTemplate": "string"
               }


            """;

        StringBuilder userBuilder = new StringBuilder();
        userBuilder.append("Suite: ").append(request.getSuiteName()).append("\n");
        userBuilder.append("Test: ").append(request.getTestName()).append("\n");
        userBuilder.append("Feature: ").append(request.getFeature()).append("\n");
        userBuilder.append("Environment: ").append(request.getEnvironment()).append("\n\n");
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
            response.setClassifiedFailureType(json.path("classifiedFailureType").asText("UNKNOWN"));
            response.setRootCauseSummary(json.path("rootCauseSummary").asText(llmRaw));
            response.setRecommendedNextSteps(json.path("recommendedNextSteps").asText(""));
            response.setSeverityScore(json.path("severityScore").asInt(3));
            response.setJiraSummaryTemplate(json.path("jiraSummaryTemplate").asText(""));
        } catch (Exception e) {
            // If parsing fails, just dump the raw content into rootCauseSummary.
            response.setClassifiedFailureType("PARSING_ERROR");
            response.setRootCauseSummary(llmRaw);
            response.setRecommendedNextSteps("LLM responded in an unexpected format.");
            response.setSeverityScore(2);
            response.setJiraSummaryTemplate("Investigate failure in test " + request.getTestName());
        }

        return response;

    }

}
