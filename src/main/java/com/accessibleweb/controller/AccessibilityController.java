package com.accessibleweb.controller;

import com.accessibleweb.service.ColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/accessibility")  // New base path
@CrossOrigin(origins = "http://localhost:3000")
public class AccessibilityController {

    @Autowired
    private ColorService colorService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestParam String url,
            @RequestParam(required = false, defaultValue = "default") String mode
    ) {
        Map<String, Object> result = colorService.analyzeAccessibility(url, mode);
        System.out.println("=== SENDING TO FRONTEND ===");
        result.forEach((key, value) -> System.out.println(key + ": " + value));
        return ResponseEntity.ok(result);
    }
}