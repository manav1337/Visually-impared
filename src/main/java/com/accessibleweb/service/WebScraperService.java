package com.accessibleweb.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WebScraperService {

    public Map<String, String> extractColors(String url) {
        Map<String, String> colors = new LinkedHashMap<>();

        try {
            // Validate URL
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("URL cannot be empty");
            }

            // Configure Jsoup connection with timeout and user agent
            Document doc = Jsoup.connect(url)
                               .timeout(10000)
                               .userAgent("Chrome/120.0.0.0")
                               .get();

            /*System.out.println(doc.html());*/

            // Extract inline styles (Body background)
            Element body = doc.body();
            String bgColor = body.attr("bgcolor");
            String bodyStyle = body.attr("style");


            if (!bgColor.isEmpty()) colors.put("body-bgcolor", bgColor);
            String extractedBodyColor = extractColorFromStyle(bodyStyle);
            if (!extractedBodyColor.isEmpty()) colors.put("body-style", extractedBodyColor);


            // ✅ Extract colors from <h1>
            Element h1 = doc.selectFirst("h1");
            if (h1 != null) {
                String extractedH1Color = extractColorFromStyle(h1.attr("style"));
                if (!extractedH1Color.isEmpty()) colors.put("h1", extractedH1Color);
            }

            // ✅ Extract unique button colors
            Elements buttons = doc.select("button, .button, input[type=button], input[type=submit]");
            for (Element button : buttons) {
                // Try multiple methods to get the button color
                String color = extractColorFromStyle(button.attr("style"));  // 1. Check inline style

                // 2. Check computed style if no inline style found
                if (color.isEmpty()) {
                    // Try to get color from the button's classes
                    String classes = button.className();
                    if (!classes.isEmpty()) {
                        for (String className : classes.split("\\s+")) {
                            String classColor = getComputedCssProperty(doc, "." + className, "background-color");
                            if (!classColor.isEmpty()) {
                                color = classColor;
                                break;
                            }
                        }
                    }

                    // If still no color found, try direct button selector
                    if (color.isEmpty()) {
                        color = getComputedCssProperty(doc, "button", "background-color");
                    }
                }

                // 3. Check computed background-color
                if (color.isEmpty()) {
                    String computedStyle = button.attr("style");
                    if (computedStyle.contains("background-color")) {
                        Pattern bgColorPattern = Pattern.compile("background-color:\\s*([^;]+)");
                        Matcher matcher = bgColorPattern.matcher(computedStyle);

                        if (matcher.find()) {
                            color = matcher.group(1).trim();
                        }
                    }
                }

                // Add the color to uniqueButtonColors if found
                if (!color.isEmpty()) {
                    colors.put("button-" + colors.size(), color);
                }
            }

            // ✅ Extract colors from <style> tags
            Elements styles = doc.select("style");
            Pattern colorPattern = Pattern.compile("(color|background-color):\\s*([^;]+);");

            for (Element style : styles) {
                Matcher matcher = colorPattern.matcher(style.html());
                while (matcher.find()) {
                    colors.put(matcher.group(1), matcher.group(2));
                }
            }

            // Extract colors from external CSS files
            extractExternalCss(doc, colors);

        } catch (IOException e) {
            log.error("Error fetching URL: {}", url, e);
            // Instead of returning empty map, you might want to throw a custom exception
            // throw new ScrapingException("Failed to scrape colors from URL: " + url, e);
        } catch (Exception e) {
            log.error("Unexpected error while scraping colors from URL: {}", url, e);
            // throw new ScrapingException("Unexpected error while scraping colors", e);
        }

        return colors;
    }

    private void extractExternalCss(Document doc, Map<String, String> colors) {
        Elements links = doc.select("link[rel=stylesheet]");

        Map<String, Set<String>> colorSets = new HashMap<>();
        colorSets.put("button", new LinkedHashSet<>());
        colorSets.put("background", new LinkedHashSet<>());
        colorSets.put("text", new LinkedHashSet<>());

        // First extract colors from inline styles
        extractInlineStyles(doc, colorSets);

        // Then extract from external stylesheets
        for (Element link : links) {
            String cssUrl = link.absUrl("href");
            if (!cssUrl.isEmpty()) {
                try {
                    String cssText = Jsoup.connect(cssUrl)
                        .timeout(5000)
                        .userAgent("Chrome/120.0.0.0")
                        .ignoreContentType(true)
                        .execute()
                        .body();

                    extractColorsByType(cssText, colorSets);
                    /*log.info("Extracted Colors - Buttons: {}", colorSets.get("button"));
                    log.info("Extracted Colors - Backgrounds: {}", colorSets.get("background"));
                    log.info("Extracted Colors - Text: {}", colorSets.get("text"));*/

                } catch (IOException e) {
                    System.err.println("Failed to fetch CSS file: " + cssUrl);
                }
            }
        }

        // Add colors to final map with limits
        addPrioritizedColors(colorSets.get("button"), colors, "button", 10);
        addPrioritizedColors(colorSets.get("background"), colors, "background", 15);
        addPrioritizedColors(colorSets.get("text"), colors, "text", 20);
    }

    private void extractInlineStyles(Document doc, Map<String, Set<String>> colorSets) {
        // Extract button colors
        Elements buttons = doc.select("button, .btn, input[type=button], input[type=submit], a.button, .button, [class*=btn]");
        for (Element button : buttons) {
            String style = button.attr("style");
            String bgColor = extractColorFromStyle(style, "background");
            String color = extractColorFromStyle(style, "color");

            if (!bgColor.isEmpty()) colorSets.get("button").add(normalizeColor(bgColor));
            if (!color.isEmpty()) colorSets.get("text").add(normalizeColor(color));
        }

        // Extract background colors
        Elements bgElements = doc.select("[style*=background], [bgcolor]");
        for (Element elem : bgElements) {
            String bgColor = elem.attr("bgcolor");
            String style = elem.attr("style");
            String extractedBg = extractColorFromStyle(style, "background");

            if (!bgColor.isEmpty()) colorSets.get("background").add(normalizeColor(bgColor));
            if (!extractedBg.isEmpty()) colorSets.get("background").add(normalizeColor(extractedBg));
        }

        // Extract text colors
        Elements textElements = doc.select("[style*=color]");
        for (Element elem : textElements) {
            String style = elem.attr("style");
            String color = extractColorFromStyle(style, "color");

            if (!color.isEmpty()) colorSets.get("text").add(normalizeColor(color));
        }
    }

    private void extractColorsByType(String cssText, Map<String, Set<String>> colorSets) {
        // Generic button selectors
        // Generic button selectors
        Pattern buttonPattern = Pattern.compile(
                "(?:button|\\[type=[\"']button[\"']]|\\[type=[\"']submit[\"']]|" +
                        "\\.[^{]*btn[^{]*|\\.[^{]*button[^{]*)[^{]*\\{[^}]*(?:background|background-color):\\s*([^;}]+)"
        );

// Generic background selectors
        Pattern bgPattern = Pattern.compile(
                "(?:body|main|header|nav|section|div|article|aside|footer|" +
                        "\\.[^{]*background[^{]*|\\.[^{]*bg[^{]*)[^{]*\\{[^}]*(?:background|background-color):\\s*([^;}]+)"
        );

// Generic text selectors
        Pattern textPattern = Pattern.compile(
                "(?:body|p|h[1-6]|span|a|div|\\.[^{]*text[^{]*)[^{]*\\{[^}]*color:\\s*([^;}]+)"
        );



        // Extract and normalize colors
        extractMatchingColors(cssText, buttonPattern, colorSets.get("button"));
        extractMatchingColors(cssText, bgPattern, colorSets.get("background"));
        extractMatchingColors(cssText, textPattern, colorSets.get("text"));
    }

    private void extractMatchingColors(String cssText, Pattern pattern, Set<String> colorSet) {
        Matcher matcher = pattern.matcher(cssText);
        while (matcher.find()) {
            String color = normalizeColor(matcher.group(1));
            if (isValidColor(color) && !isGenericColor(color)) {
                colorSet.add(color);
            }
        }
    }

    private String extractColorFromStyle(String style) {
        return extractColorFromStyle(style, "(?:background-color|background|color)");
    }

    private String extractColorFromStyle(String style, String property) {
        if (style == null || style.isEmpty()) return "";

        Pattern colorPattern = Pattern.compile(
            property + "(?:-color)?\\s*:\\s*([^;]+)"
        );

        Matcher matcher = colorPattern.matcher(style);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String normalizeColor(String color) {
        if (color == null || color.isEmpty()) return "";

        color = color.toLowerCase().trim().replace("!important", "").trim();

        // Convert named colors to hex
        Map<String, String> namedColors = Map.of(
            "white", "#ffffff",
            "black", "#000000",
            "red", "#ff0000",
            "blue", "#0000ff",
            "green", "#008000",
            "yellow", "#ffff00",
            "purple", "#800080",
            "gray", "#808080",
            "grey", "#808080"
        );

        if (namedColors.containsKey(color)) {
            return namedColors.get(color);
        }

        // Convert rgb/rgba to hex when possible
        if (color.startsWith("rgb")) {
            try {
                Pattern rgbPattern = Pattern.compile("rgba?\\s*\\((\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*(?:,\\s*[\\d.]+\\s*)?\\)");
                Matcher matcher = rgbPattern.matcher(color);
                if (matcher.matches()) {
                    int r = Integer.parseInt(matcher.group(1));
                    int g = Integer.parseInt(matcher.group(2));
                    int b = Integer.parseInt(matcher.group(3));
                    return String.format("#%02x%02x%02x", r, g, b);
                }
            } catch (Exception e) {
                // Return original if conversion fails
            }
        }

        return color;
    }

    private boolean isValidColor(String color) {
        if (color.isEmpty()) return false;

        // Filter out invalid values
        Set<String> invalidValues = Set.of(
            "transparent", "inherit", "initial", "unset", "none",
            "auto", "currentcolor", "var", "-"
        );

        for (String invalid : invalidValues) {
            if (color.contains(invalid)) return false;
        }

        // Check valid formats
        return color.matches("^#[0-9a-f]{3,8}$") || // Hex colors
               color.matches("^rgb\\s*\\([\\d\\s,]+\\)$") || // RGB
               color.matches("^rgba\\s*\\([\\d\\s,.]+\\)$") || // RGBA
               color.matches("^hsl\\s*\\([\\d\\s,%]+\\)$") || // HSL
               color.matches("^hsla\\s*\\([\\d\\s,%]+\\)$"); // HSLA
    }

    private boolean isGenericColor(String color) {
        // Check for very light/white or very dark/black colors
        return color.matches("#f{3,6}") || color.equals("#ffffff") ||
                color.matches("#0{3,6}") || color.equals("#000000");
    }

    private void addPrioritizedColors(Set<String> colorSet, Map<String, String> colors, String prefix, int limit) {
        int count = 0;
        Set<String> addedColors = new HashSet<>();

        for (String color : colorSet) {
            if (count >= limit) break;

            // Skip if we've already added a similar color
            boolean isSimilar = addedColors.stream()
                .anyMatch(added -> areSimilarColors(added, color));

            if (!isSimilar) {
                colors.put(prefix + "-" + count, color);
                addedColors.add(color);
                count++;
            }
        }
    }

    private String getComputedCssProperty(Document doc, String selector, String property) {
        Elements styles = doc.select("style");
        Pattern pattern = Pattern.compile(
            selector + "\\s*\\{[^}]*" + property + "\\s*:\\s*((?:rgb\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)|#[0-9a-fA-F]{3,6}|[a-zA-Z]+))");

        for (Element style : styles) {
            Matcher matcher = pattern.matcher(style.html());
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return "";
    }

    private boolean areSimilarColors(String color1, String color2) {
        // Normalize both colors
        color1 = normalizeColor(color1);
        color2 = normalizeColor(color2);

        // If they're exactly the same after normalization
        if (color1.equals(color2)) return true;

        // Convert both to hex format if possible
        String hex1 = convertToHex(color1);
        String hex2 = convertToHex(color2);

        // If either conversion failed, they're not similar
        if (hex1.isEmpty() || hex2.isEmpty()) return false;

        // Compare the RGB values
        return compareHexColors(hex1, hex2);
    }

    private String convertToHex(String color) {
        // If already hex, normalize to 6 digits
        if (color.startsWith("#")) {
            if (color.length() == 4) {
                // Convert #RGB to #RRGGBB
                return "#" + color.charAt(1) + color.charAt(1) +
                           color.charAt(2) + color.charAt(2) +
                           color.charAt(3) + color.charAt(3);
            }
            return color;
        }

        // Try to convert RGB/RGBA format
        if (color.startsWith("rgb")) {
            try {
                Pattern rgbPattern = Pattern.compile("rgba?\\s*\\((\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*(?:,\\s*[\\d.]+\\s*)?\\)");
                Matcher matcher = rgbPattern.matcher(color);
                if (matcher.matches()) {
                    int r = Integer.parseInt(matcher.group(1));
                    int g = Integer.parseInt(matcher.group(2));
                    int b = Integer.parseInt(matcher.group(3));
                    return String.format("#%02x%02x%02x", r, g, b);
                }
            } catch (Exception e) {
                return "";
            }
        }

        return "";
    }

    private boolean compareHexColors(String hex1, String hex2) {
        try {
            // Extract RGB components
            int r1 = Integer.parseInt(hex1.substring(1, 3), 16);
            int g1 = Integer.parseInt(hex1.substring(3, 5), 16);
            int b1 = Integer.parseInt(hex1.substring(5, 7), 16);

            int r2 = Integer.parseInt(hex2.substring(1, 3), 16);
            int g2 = Integer.parseInt(hex2.substring(3, 5), 16);
            int b2 = Integer.parseInt(hex2.substring(5, 7), 16);

            // Calculate color difference using a simple distance formula
            double difference = Math.sqrt(
                Math.pow(r1 - r2, 2) +
                Math.pow(g1 - g2, 2) +
                Math.pow(b1 - b2, 2)
            );

            // Colors are considered similar if their difference is small
            return difference < 30; // Adjust this threshold as needed

        } catch (Exception e) {
            return false;
        }
    }

}
