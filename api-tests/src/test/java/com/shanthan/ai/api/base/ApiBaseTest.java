package com.shanthan.ai.api.base;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.Reporter;

import java.io.IOException;

/**
 * Lightweight base class for API tests.
 * - Provides shared OkHttp client
 * - Helpers for GET/POST that track request/response details for listeners
 */
public abstract class ApiBaseTest {

    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // API under test base URL (override with -Dapi.baseUrl=http://host:port)
    protected final String apiBaseUrl = System.getProperty("api.baseUrl", "http://localhost:8085");

    protected final OkHttpClient httpClient = new OkHttpClient();

    // Request/response context for listeners
    protected String lastEndpoint;
    protected String lastMethod;
    protected String lastRequestBody;
    protected Response lastResponse;

    protected Response doPost(String path, String jsonBody) throws IOException {
        this.lastEndpoint = apiBaseUrl + path;
        this.lastMethod = "POST";
        this.lastRequestBody = jsonBody;

        Request request = new Request.Builder()
                .url(this.lastEndpoint)
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        Response response = httpClient.newCall(request).execute();
        this.lastResponse = response;
        Reporter.log("API POST " + lastEndpoint + " -> HTTP " + response.code(), true);
        return response;
    }

    protected Response doGet(String path) throws IOException {
        this.lastEndpoint = apiBaseUrl + path;
        this.lastMethod = "GET";
        this.lastRequestBody = null;

        Request request = new Request.Builder()
                .url(this.lastEndpoint)
                .get()
                .build();

        Response response = httpClient.newCall(request).execute();
        this.lastResponse = response;
        Reporter.log("API GET " + lastEndpoint + " -> HTTP " + response.code(), true);
        return response;
    }

    protected String bodyOrEmpty(Response response) throws IOException {
        if (response == null || response.body() == null) return "";
        return response.body().string();
    }
}
