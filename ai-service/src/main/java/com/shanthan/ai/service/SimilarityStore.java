package com.shanthan.ai.service;

import com.shanthan.ai.model.SimilarFailure;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Very small in-memory store for similar failures. In a real setup this would
 * be backed by vector search (pgvector, Qdrant, etc.).
 */
@Component
public class SimilarityStore {

    private final List<SimilarFailure> seeded = new ArrayList<>();

    public SimilarityStore() {
        // Seed with a couple of examples to make the demo feel realistic.
        seeded.add(new SimilarFailure(
                "WF-101",
                "Login button click fails on slow/staging environment",
                "Element not interactable due to missing explicit wait",
                "https://your-jira-or-ado-instance/browse/WF-101"));

        seeded.add(new SimilarFailure(
                "WF-202",
                "Checkout API returns intermittent 500 error",
                "Backend service instability in payments microservice",
                "https://your-jira-or-ado-instance/browse/WF-202"));
    }

    public List<SimilarFailure> findSimilar(String failureMessage, String stackTrace) {
        // For the exercise we just return all seeded examples.
        // You can refine later with keyword-based filtering.
        return seeded;
    }
}
