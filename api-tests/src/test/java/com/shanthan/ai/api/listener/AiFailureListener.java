package com.shanthan.ai.api.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthan.ai.model.FailureEventPayload;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

/**
 * Sends API test failures to the AI analysis service.
 * Reuses the shared FailureAnalysisRequest model from ai-service.
 */
public class AiFailureListener implements ITestListener {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = System.getProperty("ai.service.url", "http://localhost:8085");

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getName();
        System.out.println("DEBUG >>> [API Listener] onTestFailure for: " + testName);

        try {
            FailureEventPayload payload = buildPayload(result);
            String json = mapper.writeValueAsString(payload);

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/ai/analyze-failure")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                Reporter.log("AI analysis for API test '" + testName + "' (HTTP " + response.code() + "):", true);
                Reporter.log(body, true);
            }
        } catch (Exception e) {
            Reporter.log("AI triage call failed for API test '" + testName + "': " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private FailureEventPayload buildPayload(ITestResult result) {
        FailureEventPayload req = new FailureEventPayload();
        req.setTestName(result.getName());
        req.setSuiteName(result.getTestContext().getSuite().getName());
        req.setFeature(result.getTestClass().getName());
        req.setEnvironment(System.getProperty("env", "local"));

        Throwable t = result.getThrowable();
        req.setFailureMessage(t != null ? t.getMessage() : "Unknown failure");
        req.setStackTrace(stackTraceToString(t));

        // Optional metadata from test attributes (populated in ApiBaseTest subclasses)
        Object endpoint = result.getAttribute("endpoint");
        Object statusCode = result.getAttribute("statusCode");
        Object responseBody = result.getAttribute("responseBody");
        Object requestBody = result.getAttribute("requestBody");

        StringBuilder rawSnippet = new StringBuilder();
        if (endpoint != null) rawSnippet.append("Endpoint: ").append(endpoint).append("\n");
        if (statusCode != null) rawSnippet.append("Status: ").append(statusCode).append("\n");
        if (requestBody != null) rawSnippet.append("Request: ").append(requestBody).append("\n");
        if (responseBody != null) rawSnippet.append("Response: ").append(responseBody).append("\n");
        req.setRawLogSnippet(rawSnippet.toString());

        return req;
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
