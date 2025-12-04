package com.shanthan.ai.ui.model;
import java.util.List;

public class FailureEventPayload {

    private String testName;
    private String SuiteName;
    private String errorMessage;
    private String stackTrace;
    private String feature;
    private String environment;
    private List<String> tags;
    private String rawSnippet;


    public String getTestName() {
        return testName;
    }
    public void setTestName(String testName) {
        this.testName = testName;
    }
    public String getSuiteName() {
        return SuiteName;   
    }
    public void setSuiteName(String suiteName) {
        SuiteName = suiteName;
    }
    public String getErrorMessage() {
        return errorMessage;
    }       
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
    public String getRawSnippet() {
        return rawSnippet;
    }
    public void setRawSnippet(String rawSnippet) {
        this.rawSnippet = rawSnippet;
    }
    
}
