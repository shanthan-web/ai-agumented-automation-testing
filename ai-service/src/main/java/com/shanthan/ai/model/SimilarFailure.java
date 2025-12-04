package com.shanthan.ai.model;

public class SimilarFailure {

    private String id;
    private String shortDescription;
    private String suspectedRootCause;
    private String link;

    public SimilarFailure() {}

    public SimilarFailure(String id, String shortDescription, String suspectedRootCause, String link) {
        this.id = id;
        this.shortDescription = shortDescription;
        this.suspectedRootCause = suspectedRootCause;
        this.link = link;
    }

    public String getId() {
        return id;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getSuspectedRootCause() {
        return suspectedRootCause;
    }

    public String getLink() {
        return link;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setSuspectedRootCause(String suspectedRootCause) {
        this.suspectedRootCause = suspectedRootCause;
    }

    public void setLink(String link) {
        this.link = link;
    }

}
