package com.shanthan.ai.controller;

import com.shanthan.ai.model.FailureAnalysisResponse;
import com.shanthan.ai.model.FailureEventPayload;
import com.shanthan.ai.service.FailureAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class FailureAnalysisController {

    private final FailureAnalysisService failureAnalysisService;

    public FailureAnalysisController(FailureAnalysisService failureAnalysisService) {
        this.failureAnalysisService = failureAnalysisService;
    }

    @PostMapping("/analyze-failure")
    public FailureAnalysisResponse analyzeFailure(@RequestBody FailureEventPayload request) {
        System.out.println("DEBUG >>> Received failure analysis request for test: "
                + request.getTestName());
        return failureAnalysisService.analyzeFailure(request);
    }

}
