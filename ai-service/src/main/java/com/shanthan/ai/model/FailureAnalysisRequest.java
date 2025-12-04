package com.shanthan.ai.model;

import java.util.List;

public class FailureAnalysisRequest {

    private String testName;
    private String suiteName;
    private String failureMessage;
    private String stackTrace;
    private String feature;
    private String environment;
    private List<String> tags;
    private String rawLogSnippet;

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getRawLogSnippet() {
        return rawLogSnippet;
    }

    public void setRawLogSnippet(String rawLogSnippet) {
        this.rawLogSnippet = rawLogSnippet;
    }

}
