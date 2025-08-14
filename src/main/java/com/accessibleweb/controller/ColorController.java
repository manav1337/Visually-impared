package com.accessibleweb.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.accessibleweb.service.ColorService;

@RestController
@RequestMapping("/api/colors")
@CrossOrigin(origins = "http://localhost:3000") // More secure than *
public class ColorController {

    @Autowired
    private ColorService colorService; // Note the variable name matches the class

    @GetMapping
    public ResponseEntity<Map<String, String>> getExtractedColors(@RequestParam String url) {
        Map<String, String> colors = colorService.extractColorsFromWebsite(url);

        // Add this debug log:
        System.out.println("=== SENDING TO FRONTEND ===");
        colors.forEach((key, value) -> System.out.println(key + ": " + value));

        return ResponseEntity.ok(colors);
    }
}
