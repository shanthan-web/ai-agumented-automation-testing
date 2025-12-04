package com.shanthan.ai.model;

import lombok.*;
import java.util.List;

@Getter
@Setter
public class FailureAnalysisResponse {

    private String classifiedFailureType;   // e.g., LOCATOR_ISSUE, ENVIRONMENT, BACKEND_500
    private String rootCauseSummary;
    private String recommendedNextSteps;
    private int severityScore;              // 1–5
    private String jiraSummaryTemplate;
    private List<SimilarFailure> similarFailures;

}
