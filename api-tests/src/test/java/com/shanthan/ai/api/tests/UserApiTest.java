package com.shanthan.ai.api.tests;

import com.shanthan.ai.api.base.ApiBaseTest;
import okhttp3.Response;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * Intentional failing API test so the AI analysis is exercised.
 */
public class UserApiTest extends ApiBaseTest {

    @Test
    public void createUser_shouldReturn201_butWeExpect200_toTriggerAI() throws IOException {
        String path = "/api/ai/analyze-failure"; // adjust to your API under test
        String requestBody = """
            {
              "name": "John Doe",
              "email": "john@example.com"
            }
            """;

        Response res = doPost(path, requestBody);

        // Attach API details as attributes so AiFailureListener can send them
        ITestResult tr = Reporter.getCurrentTestResult();
        tr.setAttribute("httpMethod", lastMethod);
        tr.setAttribute("endpoint", lastEndpoint);
        tr.setAttribute("statusCode", res.code());
        tr.setAttribute("requestBody", requestBody);
        tr.setAttribute("responseBody", bodyOrEmpty(res));

        // Intentionally wrong assertion to cause FAIL -> trigger AI triage
        assertEquals(res.code(), 200, "Expecting 200 for demo (will fail if 201).");
    }
}
