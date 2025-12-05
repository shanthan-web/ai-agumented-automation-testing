package com.shanthan.ai.ui.model;
import java.util.List;

public class FailureEventPayload {

    private String testName;
    private String suiteName;
    private String feature;
    private String environment;
    private String failureMessage;
    private String stackTrace;
    private String rawLogSnippet;
    private List<String> tags;

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

    public String getRawLogSnippet() {
        return rawLogSnippet;
    }

    public void setRawLogSnippet(String rawLogSnippet) {
        this.rawLogSnippet = rawLogSnippet;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
}
