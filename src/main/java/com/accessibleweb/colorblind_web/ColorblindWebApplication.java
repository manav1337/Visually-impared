package com.accessibleweb.colorblind_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.accessibleweb")  // ✅ Ensures it scans the controller
public class ColorblindWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(ColorblindWebApplication.class, args);
    }
}
