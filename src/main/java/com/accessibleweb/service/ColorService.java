package com.accessibleweb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ColorService {

    private final WebScraperService webScraperService;

    @Autowired
    public ColorService(WebScraperService webScraperService) {
        this.webScraperService = webScraperService;
    }

    public Map<String, String> extractColorsFromWebsite(String url) {
        return webScraperService.extractColors(url);
    }

    public Map<String, Object> analyzeAccessibility(String url, String mode) {
        String analysisMode = (mode == null) ? "default" : mode.toLowerCase();
        boolean isStrictMode = "strict".equals(analysisMode);
        Map<String, String> colors = webScraperService.extractColors(url);

        // Get the most relevant colors
        String bgColor = colors.getOrDefault("background-color",
                findDominantColor(colors, "background"));
        String textColor = colors.getOrDefault("color",
                colors.getOrDefault("text-0",
                        findDominantColor(colors, "text")));

        double contrastRatio = calculateWCAGContrast(bgColor, textColor);
        String rating = getWCAGRating(contrastRatio, isStrictMode);
        boolean colorblindSafe = isColorblindSafe(bgColor, textColor, isStrictMode);
        List<String> warnings = detectAccessibilityIssues(bgColor, textColor, contrastRatio);
        boolean lowLightVisible = !isLowLight(bgColor, textColor);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysisMode", analysisMode);

        // Color Analysis
        Map<String, Object> colorAnalysis = new LinkedHashMap<>();
        colorAnalysis.put("primaryColors", Map.of(
                "background", createColorInfo(bgColor, colors, "background-color"),
                "text", createColorInfo(textColor, colors, "color", "text-0")
        ));
        colorAnalysis.put("contrast", createContrastInfo(contrastRatio, rating, isStrictMode));
        result.put("colorAnalysis", colorAnalysis);

        // Accessibility Assessment
        result.put("accessibility", Map.of(
                "summary", getSummary(rating),
                "warnings", warnings,
                "checks", Map.of(
                        "colorblindSafe", colorblindSafe,
                        "lowLightVisible", lowLightVisible,
                        "minimumContrast", getContrastLevel(contrastRatio)
                )
        ));

        // Raw Data (organized)
        result.put("rawData", groupColorsByType(colors));

        return result;
    }

    public Map<String, Object> analyzeAccessibility(String url) {
        return analyzeAccessibility(url, "default");
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> createColorInfo(String color, Map<String, String> colors, String... keys) {
        String source = "derived";
        for (String key : keys) {
            if (colors.containsKey(key)) {
                source = "explicit";
                break;
            }
        }
        return Map.of(
                "value", color,
                "source", source
        );
    }

    private Map<String, Object> createContrastInfo(double ratio, String rating, boolean isStrict) {
        return Map.of(
                "ratio", Math.round(ratio * 100) / 100.0,
                "rating", rating,
                "meetsRequirements", Map.of(
                        "AA", ratio >= 4.5,
                        "AAA", ratio >= 7.0,
                        "strict", ratio >= 5.0
                )
        );
    }

    private String getSummary(String rating) {
        return switch (rating) {
            case "AAA" -> "Excellent";
            case "AA" -> "Good";
            default -> "Needs Improvement";
        };
    }

    private String getContrastLevel(double ratio) {
        if (ratio >= 7.0) return "exceeds";
        if (ratio >= 4.5) return "meets";
        return "fails";
    }

    private Map<String, List<String>> groupColorsByType(Map<String, String> colors) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        grouped.put("backgrounds", colors.entrySet().stream()
                .filter(e -> e.getKey().startsWith("background"))
                .map(Map.Entry::getValue)
                .distinct()
                .collect(Collectors.toList()));

        grouped.put("textColors", colors.entrySet().stream()
                .filter(e -> e.getKey().startsWith("text") || e.getKey().equals("color"))
                .map(Map.Entry::getValue)
                .distinct()
                .collect(Collectors.toList()));

        grouped.put("buttons", colors.entrySet().stream()
                .filter(e -> e.getKey().startsWith("button"))
                .map(Map.Entry::getValue)
                .distinct()
                .collect(Collectors.toList()));

        // Add remaining colors
        List<String> others = colors.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("background") &&
                        !e.getKey().startsWith("text") &&
                        !e.getKey().equals("color") &&
                        !e.getKey().startsWith("button"))
                .map(Map.Entry::getValue)
                .distinct()
                .collect(Collectors.toList());

        if (!others.isEmpty()) {
            grouped.put("other", others);
        }

        return grouped;
    }

    private double calculateWCAGContrast(String hex1, String hex2) {
        int[] rgb1 = hexToRgb(hex1);
        int[] rgb2 = hexToRgb(hex2);

        double l1 = calculateRelativeLuminance(rgb1);
        double l2 = calculateRelativeLuminance(rgb2);

        return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
    }

    private int[] hexToRgb(String hex) {
        try {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            return new int[] {
                    Integer.parseInt(cleanHex.substring(0, 2), 16),
                    Integer.parseInt(cleanHex.substring(2, 4), 16),
                    Integer.parseInt(cleanHex.substring(4, 6), 16)
            };
        } catch (Exception e) {
            return new int[]{255, 255, 255}; // Default to white on error
        }
    }

    private double calculateRelativeLuminance(int[] rgb) {
        double r = rgb[0] / 255.0;
        double g = rgb[1] / 255.0;
        double b = rgb[2] / 255.0;

        r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private String getWCAGRating(double ratio, boolean isStrict) {
        if (ratio >= 7.0) return "AAA";
        if (ratio >= (isStrict ? 5.0 : 4.5)) return "AA";
        return "Fail";
    }

    private String findDominantColor(Map<String, String> colors, String type) {
        // First try exact matches
        String exactMatch = colors.get(type);
        if (exactMatch != null) return exactMatch;

        // Then try common variants
        String variantMatch = colors.get(type + "-color");
        if (variantMatch != null) return variantMatch;

        // Fallback to frequency analysis
        return colors.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(type.toLowerCase()))
                .max(Comparator.comparingLong(e -> getColorFrequency(colors, e.getValue())))
                .map(Map.Entry::getValue)
                .orElseGet(() -> type.equalsIgnoreCase("background") ? "#FFFFFF" : "#000000");
    }

    private long getColorFrequency(Map<String, String> colors, String target) {
        return colors.values().stream()
                .filter(c -> c.equalsIgnoreCase(target))
                .count();
    }

    private boolean isColorblindSafe(String c1, String c2, boolean isStrict) {
        if (isProblematicCombo(c1, c2)) return false;
        if (isStrict) {
            return calculateColorDifference(c1, c2) > 500;
        }
        return true;
    }

    private boolean isProblematicCombo(String c1, String c2) {
        Set<String> reds = Set.of("#ff0000", "#cc0000", "#ff3333", "#c00");
        Set<String> greens = Set.of("#00ff00", "#00cc00", "#33ff33", "#0c0");
        Set<String> blues = Set.of("#0000ff", "#0000cc", "#3333ff", "#00c");

        String lc1 = c1.toLowerCase();
        String lc2 = c2.toLowerCase();

        return (reds.contains(lc1) && greens.contains(lc2)) ||
                (reds.contains(lc2) && greens.contains(lc1)) ||
                (blues.contains(lc1) && lc2.contains("ff0")) ||
                (blues.contains(lc2) && lc1.contains("ff0"));
    }

    private List<String> detectAccessibilityIssues(String bg, String text, double contrast) {
        List<String> issues = new ArrayList<>();

        // Contrast suggestions
        if (contrast > 4.5 && contrast < 7.0) {
            issues.add("Good contrast (AA), but could be improved to AAA (currently "
                    + Math.round(contrast * 100)/100.0 + " of 7.0 required)");
        }

        // Colorblind edge cases
        if (isPotentialIssue(bg, text)) {
            issues.add("Colors might be problematic for certain types of color blindness");
        }

        // Low light check
        if (isLowLight(bg, text)) {
            issues.add("Consider testing in low-light conditions");
        }

        return issues;
    }

    private boolean isPotentialIssue(String c1, String c2) {
        // Check for subtle problematic combinations
        return calculateColorDifference(c1, c2) < 300 &&
                !isHighContrast(c1, c2);
    }

    private boolean isHighContrast(String c1, String c2) {
        return calculateWCAGContrast(c1, c2) > 7.0;
    }

    private boolean isLowLight(String c1, String c2) {
        return isDarkColor(c1) && isDarkColor(c2);
    }

    private boolean isDarkColor(String hex) {
        int[] rgb = hexToRgb(hex);
        double brightness = (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]) / 255;
        return brightness < 0.4;
    }

    private double calculateColorDifference(String hex1, String hex2) {
        int[] rgb1 = hexToRgb(hex1);
        int[] rgb2 = hexToRgb(hex2);

        // Use CIELAB color space for better perceptual difference
        double[] lab1 = rgbToLab(rgb1);
        double[] lab2 = rgbToLab(rgb2);

        return Math.sqrt(
                Math.pow(lab1[0] - lab2[0], 2) +
                        Math.pow(lab1[1] - lab2[1], 2) +
                        Math.pow(lab1[2] - lab2[2], 2)
        );
    }

    private double[] rgbToLab(int[] rgb) {
        // Convert RGB to CIELAB (simplified)
        double r = rgb[0] / 255.0;
        double g = rgb[1] / 255.0;
        double b = rgb[2] / 255.0;

        // Simplified conversion for demonstration
        return new double[] {
                0.2126 * r + 0.7152 * g + 0.0722 * b, // L
                r - g, // a
                b - (r + g) / 2  // b
        };
    }
}