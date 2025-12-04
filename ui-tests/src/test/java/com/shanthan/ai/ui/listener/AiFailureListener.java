package com.shanthan.ai.ui.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthan.ai.ui.model.FailureEventPayload;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

public class AiFailureListener implements ITestListener {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // You can externalize this via system property if you like.
    private final String aiServiceBaseUrl = System.getProperty("ai.service.url", "http://localhost:8085");

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("DEBUG >>> AiFailureListener triggered for: " + result.getName());
        ITestContext context = result.getTestContext();
        String testName = result.getName();
        String suiteName = context.getSuite().getName();
        String failureMessage = result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "Unknown failure";

        StringWriter sw = new StringWriter();
        if (result.getThrowable() != null) {
            result.getThrowable().printStackTrace(new PrintWriter(sw));
        }

        FailureEventPayload payload = new FailureEventPayload();
        payload.setSuiteName(suiteName);
        payload.setTestName(testName);
        payload.setErrorMessage(failureMessage);
        payload.setStackTrace(sw.toString());
        payload.setFeature("Login");
        payload.setEnvironment(System.getProperty("env", "local"));
        payload.setTags(List.of("ui", "login"));
        payload.setRawSnippet("Last URL: " + safeGetCurrentUrl(result));

        try {
            String json = mapper.writeValueAsString(payload);
            Request request = new Request.Builder()
                    .url(aiServiceBaseUrl + "/api/ai/analyze-failure")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                Reporter.log("AI Analysis for test '" + testName + "':", true);
                Reporter.log(body, true);
            }
        } catch (Exception e) {
            Reporter.log("Failed to call AI service: " + e.getMessage(), true);
        }

    }

    private String safeGetCurrentUrl(ITestResult result) {
        try {
            Object instance = result.getInstance();
            // If BaseTest exposes getDriver(), you can downcast here if needed.
            // To keep this decoupled, just return placeholder for now.
            return "<not captured>";
        } catch (Exception e) {
            return "<not captured>";
        }
    }

    // Other methods left empty intentionally.
    @Override public void onTestStart(ITestResult result) {}
    @Override public void onTestSuccess(ITestResult result) {}
    @Override public void onTestSkipped(ITestResult result) {}
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onTestFailedWithTimeout(ITestResult result) { onTestFailure(result); }
    @Override public void onStart(ITestContext context) {}
    @Override public void onFinish(ITestContext context) {}
}
