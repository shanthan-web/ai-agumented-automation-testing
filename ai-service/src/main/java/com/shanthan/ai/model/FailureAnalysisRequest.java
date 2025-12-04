package com.shanthan.ai.model;

import lombok.*;
import java.util.List;

@Getter
@Setter
public class FailureAnalysisRequest {

    private String testName;
    private String suiteName;
    private String failureMessage;
    private String stackTrace;
    private String feature;
    private String environment;
    private List<String> tags;
    private String rawLogSnippet;

}
