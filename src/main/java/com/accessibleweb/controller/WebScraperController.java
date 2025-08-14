package com.accessibleweb.controller;

import com.accessibleweb.model.CustomErrorResponse;
import com.accessibleweb.service.WebScraperService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api")  // ✅ Base path for all endpoints in this controller
public class WebScraperController {

    private final WebScraperService webScraperService;

    public WebScraperController(WebScraperService webScraperService) {
        this.webScraperService = webScraperService;
    }

    @GetMapping("/scraper")  // ✅ This makes the endpoint available at /api/scraper
    public ResponseEntity<?> scrapeColors(@RequestParam String url) {
        try {
            Map<String, String> colors = webScraperService.extractColors(url);
            if (colors.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(colors);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CustomErrorResponse("Invalid URL provided"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(new CustomErrorResponse("Error processing request: " + e.getMessage()));
        }
    }
}



