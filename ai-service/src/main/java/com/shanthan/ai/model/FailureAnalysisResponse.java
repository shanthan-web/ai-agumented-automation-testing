package com.shanthan.ai.model;

import java.util.List;

public class FailureAnalysisResponse {

    private String classifiedFailureType;   // e.g., LOCATOR_ISSUE, ENVIRONMENT, BACKEND_500
    private String rootCauseSummary;
    private String recommendedNextSteps;
    private int severityScore;              // 1–5
    private String jiraSummaryTemplate;
    private List<SimilarFailure> similarFailures;

    public String getClassifiedFailureType() {
        return classifiedFailureType;
    }

    public void setClassifiedFailureType(String classifiedFailureType) {
        this.classifiedFailureType = classifiedFailureType;
    }

    public String getRootCauseSummary() {
        return rootCauseSummary;
    }

    public void setRootCauseSummary(String rootCauseSummary) {
        this.rootCauseSummary = rootCauseSummary;
    }

    public String getRecommendedNextSteps() {
        return recommendedNextSteps;
    }

    public void setRecommendedNextSteps(String recommendedNextSteps) {
        this.recommendedNextSteps = recommendedNextSteps;
    }

    public int getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(int severityScore) {
        this.severityScore = severityScore;
    }

    public String getJiraSummaryTemplate() {
        return jiraSummaryTemplate;
    }

    public void setJiraSummaryTemplate(String jiraSummaryTemplate) {
        this.jiraSummaryTemplate = jiraSummaryTemplate;
    }

    public List<SimilarFailure> getSimilarFailures() {
        return similarFailures;
    }

    public void setSimilarFailures(List<SimilarFailure> similarFailures) {
        this.similarFailures = similarFailures;
    }
}
