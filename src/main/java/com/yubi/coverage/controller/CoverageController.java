package com.yubi.coverage.controller;

import com.yubi.coverage.service.CoverageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/coverage")
public class CoverageController {

    private final CoverageService coverageService;

    @Autowired
    public CoverageController(CoverageService coverageService) {
        this.coverageService = coverageService;
    }

    /**
     * Generates and returns the path to the HTML coverage report
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, String>> generateReport() {
        try {
            String reportPath = coverageService.generateCoverageReport();
            Map<String, String> response = new HashMap<>();
            response.put("reportPath", reportPath);
            response.put("message", "Coverage report generated successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate coverage report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Saves the current execution data to a file and returns the file path
     */
    @GetMapping("/save")
    public ResponseEntity<Map<String, String>> saveExecutionData() {
        try {
            String filePath = coverageService.saveExecutionData();
            Map<String, String> response = new HashMap<>();
            response.put("filePath", filePath);
            response.put("message", "Execution data saved successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to save execution data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Resets the coverage data in the JaCoCo agent
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetCoverage() {
        try {
            coverageService.resetCoverage();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Coverage data reset successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to reset coverage data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Checks if the JaCoCo agent is running
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean isAgentRunning = coverageService.isAgentAccessible();
        
        status.put("agentRunning", isAgentRunning);
        status.put("message", isAgentRunning ? 
            "JaCoCo agent is running and accessible on port 8008" : 
            "JaCoCo agent is not running or not accessible on port 8008");
        
        return ResponseEntity.ok(status);
    }
}
