package cn.iocoder.yudao.module.amazon.ai.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates LLM responses into structured Java objects.
 *
 * <p>Handles the common LLM quirks:
 * <ul>
 *   <li>JSON wrapped in markdown code fences (```json ... ```)</li>
 *   <li>Trailing commas and other JSON malformations</li>
 *   <li>Extra text before/after the JSON block</li>
 *   <li>Schema validation against expected fields</li>
 *   <li>Fallback parsing for non-JSON structured text</li>
 * </ul>
 *
 * <p>Ported from the omniscient project's {@code BaseLLMClient._try_parse_json} pattern,
 * adapted for Spring AI's response model.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    /** Pattern to extract JSON from markdown code fences: ```json ... ``` or ``` ... ``` */
    private static final Pattern CODE_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?\\s*```", Pattern.DOTALL);

    /** Pattern to find the first JSON object in a string */
    private static final Pattern JSON_OBJECT_PATTERN =
            Pattern.compile("\\{[\\s\\S]*}", Pattern.DOTALL);

    /** Pattern to find the first JSON array in a string */
    private static final Pattern JSON_ARRAY_PATTERN =
            Pattern.compile("\\[[\\s\\S]*]", Pattern.DOTALL);

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Parse an LLM response string into a typed Java object.
     *
     * <p>This method is resilient to common LLM output quirks:
     * - Strips markdown code fences
     * - Extracts JSON from surrounding text
     * - Cleans trailing commas
     * - Retries with lenient parsing
     *
     * @param responseText the raw LLM response
     * @param targetType   the target class
     * @param <T>          the target type
     * @return the parsed object
     * @throws AiResponseParseException if parsing fails after all recovery attempts
     */
    public <T> T parse(String responseText, Class<T> targetType) {
        if (responseText == null || responseText.isBlank()) {
            throw new AiResponseParseException("Empty LLM response");
        }

        // Attempt 1: Direct parse
        try {
            return objectMapper.readValue(responseText.trim(), targetType);
        } catch (JsonProcessingException ignored) {
            // Continue to recovery strategies
        }

        // Attempt 2: Strip markdown code fences
        String cleaned = stripCodeFences(responseText);
        if (!cleaned.equals(responseText)) {
            try {
                return objectMapper.readValue(cleaned, targetType);
            } catch (JsonProcessingException ignored) {
                // Continue
            }
        }

        // Attempt 3: Extract JSON object from surrounding text
        String extracted = extractJsonObject(responseText);
        if (extracted != null) {
            try {
                return objectMapper.readValue(extracted, targetType);
            } catch (JsonProcessingException ignored) {
                // Continue
            }
        }

        // Attempt 4: Clean trailing commas and retry
        String cleanedJson = cleanTrailingCommas(cleaned);
        try {
            return objectMapper.readValue(cleanedJson, targetType);
        } catch (JsonProcessingException ignored) {
            // Continue
        }

        // Attempt 5: Lenient deserialization (ignore unknown properties)
        try {
            ObjectMapper lenientMapper = objectMapper.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return lenientMapper.readValue(cleanedJson, targetType);
        } catch (JsonProcessingException e) {
            throw new AiResponseParseException(
                    "Failed to parse LLM response as " + targetType.getSimpleName()
                    + " after all recovery attempts. Response: "
                    + responseText.substring(0, Math.min(500, responseText.length())), e);
        }
    }

    /**
     * Parse an LLM response into a generic Map.
     */
    public Map<String, Object> parseToMap(String responseText) {
        return parse(responseText, new TypeReference<>() {});
    }

    /**
     * Parse an LLM response using a TypeReference (for generic types like List<T>).
     */
    public <T> T parse(String responseText, TypeReference<T> typeRef) {
        if (responseText == null || responseText.isBlank()) {
            throw new AiResponseParseException("Empty LLM response");
        }

        String cleaned = stripCodeFences(responseText);
        String extracted = extractJsonObject(cleaned);
        String json = extracted != null ? extracted : cleaned;
        json = cleanTrailingCommas(json);

        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new AiResponseParseException(
                    "Failed to parse LLM response. Response: "
                    + responseText.substring(0, Math.min(500, responseText.length())), e);
        }
    }

    /**
     * Validate that a parsed JSON contains all required fields.
     *
     * @param responseText   the raw JSON text
     * @param requiredFields field names that must be present
     * @return true if all required fields exist and are non-null
     */
    public boolean validateRequiredFields(String responseText, String... requiredFields) {
        try {
            String cleaned = stripCodeFences(responseText);
            String extracted = extractJsonObject(cleaned);
            String json = extracted != null ? extracted : cleaned;
            JsonNode root = objectMapper.readTree(json);

            for (String field : requiredFields) {
                if (!root.has(field) || root.get(field).isNull()) {
                    log.warn("Missing required field '{}' in LLM response", field);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to validate required fields: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract a specific field value from a JSON response as a string.
     *
     * @param responseText the raw JSON text
     * @param fieldPath    dot-separated path, e.g. "product_blueprint.target_price_point"
     * @return the field value as string, or null if not found
     */
    public String extractField(String responseText, String fieldPath) {
        try {
            String cleaned = stripCodeFences(responseText);
            String extracted = extractJsonObject(cleaned);
            String json = extracted != null ? extracted : cleaned;
            JsonNode root = objectMapper.readTree(json);

            String[] parts = fieldPath.split("\\.");
            JsonNode current = root;
            for (String part : parts) {
                if (current == null || !current.has(part)) {
                    return null;
                }
                current = current.get(part);
            }
            return current != null ? current.asText() : null;
        } catch (Exception e) {
            log.warn("Failed to extract field '{}': {}", fieldPath, e.getMessage());
            return null;
        }
    }

    /**
     * Best-effort text extraction when the LLM returns structured text instead of JSON.
     * Splits by common delimiters and returns the first meaningful block.
     */
    public String extractTextBlock(String responseText, String sectionHeader) {
        if (responseText == null) return null;

        int startIdx = responseText.indexOf(sectionHeader);
        if (startIdx < 0) return null;

        int contentStart = responseText.indexOf('\n', startIdx);
        if (contentStart < 0) return null;

        // Find the next section header or end of text
        int endIdx = responseText.length();
        for (String delimiter : new String[]{"\n##", "\n###", "\n---", "\n\n\n"}) {
            int idx = responseText.indexOf(delimiter, contentStart + 1);
            if (idx > 0 && idx < endIdx) {
                endIdx = idx;
            }
        }

        return responseText.substring(contentStart + 1, endIdx).trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Strip markdown code fences from the response.
     */
    String stripCodeFences(String text) {
        text = text.trim();
        Matcher matcher = CODE_FENCE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // Manual strip if regex doesn't match
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            return text.trim();
        }
        return text;
    }

    /**
     * Extract the first JSON object from a string that may contain surrounding text.
     */
    String extractJsonObject(String text) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        // Try array
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(text);
        if (arrayMatcher.find()) {
            return arrayMatcher.group();
        }
        return null;
    }

    /**
     * Remove trailing commas before closing brackets/braces.
     * e.g. [1, 2, 3,] -> [1, 2, 3]
     */
    String cleanTrailingCommas(String json) {
        if (json == null) return null;
        return json.replaceAll(",\\s*([}\\]])", "$1");
    }

    // -----------------------------------------------------------------------
    // Exception
    // -----------------------------------------------------------------------

    /**
     * Exception thrown when LLM response parsing fails.
     */
    public static class AiResponseParseException extends RuntimeException {
        public AiResponseParseException(String message) {
            super(message);
        }

        public AiResponseParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
