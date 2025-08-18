package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.dto.SocialPostDto;
import com.socialmedia.data.ingestion.service.DataProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*") // For frontend development
public class DataProcessingController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataProcessingController.class);
    
    @Autowired
    private DataProcessingService dataProcessingService;
    
    /**
     * Health check endpoint
     * GET /api/posts/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Data Processing Service");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple endpoint to test service connectivity
     * GET /api/posts/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Data Processing Controller is working!");
        response.put("availableEndpoints", "POST /api/posts, GET /api/posts/health, GET /api/posts/test");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Determine appropriate HTTP status based on exception type
     */
    private HttpStatus determineHttpStatus(Exception e) {
        String exceptionName = e.getClass().getSimpleName();
        
        switch (exceptionName) {
            case "DuplicatePostException":
                return HttpStatus.CONFLICT; // 409
            case "ValidationException":
                return HttpStatus.BAD_REQUEST; // 400
            case "PostNotFoundException":
                return HttpStatus.NOT_FOUND; // 404
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR; // 500
        }
    }
}