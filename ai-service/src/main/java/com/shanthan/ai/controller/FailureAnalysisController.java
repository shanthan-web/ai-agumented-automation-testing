package com.shanthan.ai.controller;

import com.shanthan.ai.model.FailureAnalysisRequest;
import com.shanthan.ai.model.FailureAnalysisResponse;
import com.shanthan.ai.service.FailureAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class FailureAnalysisController {

    private final FailureAnalysisService failureAnalysisService;

    public FailureAnalysisController(FailureAnalysisService failureAnalysisService) {
        this.failureAnalysisService = failureAnalysisService;
    }

    @PostMapping("/analyze-failure")
    public ResponseEntity<FailureAnalysisResponse> analyzeFailure(@RequestBody FailureAnalysisRequest request) {
        System.out.println("DEBUG >>> Received failure analysis request for test: "
                + request.getTestName());
        return ResponseEntity.ok(failureAnalysisService.analyzeFailure(request));
    }

}
