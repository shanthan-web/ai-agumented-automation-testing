package com.shanthan.ai.model;

import lombok.*;

@Setter
@Getter
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

}
