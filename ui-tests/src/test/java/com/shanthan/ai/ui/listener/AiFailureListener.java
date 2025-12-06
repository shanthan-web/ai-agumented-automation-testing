package com.shanthan.ai.ui.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthan.ai.model.FailureAnalysisResponse;
import com.shanthan.ai.model.FailureEventPayload;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TestNG listener that:
 *  - Listens for test failures
 *  - Sends a failure payload to the AI triage Spring Boot service
 *  - Logs a structured AI analysis block into the TestNG report/console
 */
public class AiFailureListener implements ITestListener {

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Base URL for the AI service.
     * Can be overridden via -Dai.service.url=http://host:port
     */
    private final String baseUrl =
            System.getProperty("ai.service.url", "http://localhost:8085");

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getName();

        System.out.println("DEBUG >>> [AI Listener] onTestFailure for: " + testName);

        try {
            // 1. Build the payload sent to the AI service
            FailureEventPayload payload = buildPayload(result);
            String json = mapper.writeValueAsString(payload);

            String url = baseUrl + "/api/ai/analyze-failure";
            System.out.println("DEBUG >>> [AI Listener] Calling AI service at: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json, JSON))
                    .build();

            long start = System.currentTimeMillis();

            // 2. Synchronous HTTP call (we wait here, but we do NOT change the test result)
            try (Response response = client.newCall(request).execute()) {
                long end = System.currentTimeMillis();

                String body = response.body() != null ? response.body().string() : "";
                int statusCode = response.code();

                System.out.println("DEBUG >>> [AI Listener] AI service HTTP status: " + statusCode);
                System.out.println("DEBUG >>> [AI Listener] AI service call took " + (end - start) + " ms");
                System.out.println("DEBUG >>> [AI Listener] AI service raw body: " + body);

                // 3. Try to parse into FailureAnalysisResponse; if parsing fails, just log the raw JSON
                try {
                    FailureAnalysisResponse ai =
                            mapper.readValue(body, FailureAnalysisResponse.class);

                    Reporter.log("===== AI TRIAGE FOR: " + testName + " =====", true);
                    Reporter.log("Failure type        : " + ai.getFailureType(), true);
                    Reporter.log("AI confidence       : " + ai.getAiConfidence(), true);
                    Reporter.log("Severity (1–5)      : " + ai.getSeverityScore(), true);
                    Reporter.log("Root cause (AI)     : " + ai.getRootCauseSummary(), true);
                    Reporter.log("Next steps (AI)     : " + ai.getRecommendedNextSteps(), true);
                    Reporter.log("Jira summary        : " + ai.getJiraSummaryTemplate(), true);
                    Reporter.log("Rule override?      : " + ai.isRuleBasedOverrideApplied(), true);

                    if (ai.getSimilarFailures() != null && !ai.getSimilarFailures().isEmpty()) {
                        Reporter.log("Similar failures:", true);
                        ai.getSimilarFailures().forEach(sim -> Reporter.log(" - [" + sim.getId() + "] " + sim.getShortDescription()
                                + " (root cause: " + sim.getSuspectedRootCause() + ")", true));
                    }

                    Reporter.log("=========================================", true);
                } catch (Exception parseErr) {
                    Reporter.log("===== AI TRIAGE (RAW JSON) FOR: " + testName + " =====", true);
                    Reporter.log("HTTP status         : " + statusCode, true);
                    Reporter.log(body, true);
                    Reporter.log("=========================================", true);
                }
            }
        } catch (IOException e) {
            System.out.println("DEBUG >>> [AI Listener] Error calling AI service: " + e.getMessage());
            e.printStackTrace();
            Reporter.log("AI triage call failed for test '" + testName +
                    "' – see logs for details.", true);
        } catch (Exception e) {
            System.out.println("DEBUG >>> [AI Listener] Unexpected error in AiFailureListener: " + e.getMessage());
            e.printStackTrace();
            Reporter.log("AI triage listener crashed for test '" + testName + "'.", true);
        }

    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    private FailureEventPayload buildPayload(ITestResult result) {
        FailureEventPayload p = new FailureEventPayload();
        p.setTestName(result.getName());
        p.setSuiteName(result.getTestContext().getSuite().getName());
        p.setFeature(result.getTestClass().getName());
        p.setEnvironment(System.getProperty("env", "local"));

        Throwable t = result.getThrowable();
        p.setFailureMessage(t != null ? t.getMessage() : "");
        p.setStackTrace(stackTraceToString(t));
        p.setRawLogSnippet("");
        // default type = UI unless we detect API attributes
        p.setTestType("UI");

        // Optional: tags to help grouping on the AI side
        List<String> tags = new ArrayList<>();
        tags.add(result.getTestClass().getName());
        p.setTags(tags);
        // Read API-specific attributes (if present, treat as API test)
        Object methodAttr   = result.getAttribute("httpMethod");
        Object endpointAttr = result.getAttribute("endpoint");
        Object statusAttr   = result.getAttribute("statusCode");
        Object reqAttr      = result.getAttribute("requestBody");
        Object resAttr      = result.getAttribute("responseBody");

        if (methodAttr != null || endpointAttr != null) {
            p.setTestType("API");
            p.setHttpMethod(methodAttr != null ? methodAttr.toString() : null);
            p.setEndpoint(endpointAttr != null ? endpointAttr.toString() : null);
            if (statusAttr instanceof Integer) {
                p.setStatusCode((Integer) statusAttr);
            } else if (statusAttr != null) {
                try {
                    p.setStatusCode(Integer.parseInt(statusAttr.toString()));
                } catch (NumberFormatException ignored) {}
            }
            p.setRequestBody(reqAttr != null ? reqAttr.toString() : null);
            p.setResponseBody(resAttr != null ? resAttr.toString() : null);
        }

        return p;
    }

    private String stackTraceToString(Throwable t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(t).append("\n");
        for (StackTraceElement ste : t.getStackTrace()) {
            sb.append("    at ").append(ste.toString()).append("\n");
        }
        return sb.toString();
    }

}
