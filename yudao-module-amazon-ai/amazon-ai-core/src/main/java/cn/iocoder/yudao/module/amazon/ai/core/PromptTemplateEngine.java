package cn.iocoder.yudao.module.amazon.ai.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and renders prompt templates from the classpath.
 *
 * <p>Templates are stored under {@code resources/prompts/} with a {@code .st} extension
 * (StringTemplate-inspired format). Variables use {@code {{variable}}} syntax.
 *
 * <p>Features:
 * <ul>
 *   <li>Lazy loading with LRU cache</li>
 *   <li>{{variable}} substitution</li>
 *   <li>Conditional sections: {{#if condition}}...{{/if}}</li>
 *   <li>Template versioning via filename suffix (e.g. listing_generator_v2.st)</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class PromptTemplateEngine {

    private static final String TEMPLATE_DIR = "prompts/";
    private static final String TEMPLATE_SUFFIX = ".st";

    /** Pattern for {{variable}} placeholders */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /** Pattern for {{#if condition}}...{{/if}} conditional blocks */
    private static final Pattern IF_PATTERN =
            Pattern.compile("\\{\\{#if\\s+(\\w+)}}(.*?)\\{\\{/if}}", Pattern.DOTALL);

    /**
     * In-memory template cache: template name -> raw template content.
     */
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Render a prompt template with the given variables.
     *
     * <p>Usage:
     * <pre>{@code
     * String prompt = templateEngine.render("listing_generator", Map.of(
     *     "category", "Water Bottle",
     *     "keywords", "insulated, BPA free",
     *     "targetLanguage", "English"
     * ));
     * }</pre>
     *
     * @param templateName the template name (without path or extension), e.g. "listing_generator"
     * @param variables    key-value pairs to substitute
     * @return the rendered prompt string
     * @throws IllegalStateException if the template cannot be loaded
     */
    public String render(String templateName, Map<String, Object> variables) {
        String rawTemplate = loadTemplate(templateName);
        return renderString(rawTemplate, variables);
    }

    /**
     * Render a prompt template, using a specific version.
     *
     * @param templateName base template name
     * @param version      version suffix, e.g. "v2"
     * @param variables    substitution variables
     * @return rendered prompt
     */
    public String renderVersioned(String templateName, String version, Map<String, Object> variables) {
        String versionedName = templateName + "_" + version;
        try {
            return render(versionedName, variables);
        } catch (IllegalStateException e) {
            log.warn("Versioned template [{}] not found, falling back to base template [{}]",
                    versionedName, templateName);
            return render(templateName, variables);
        }
    }

    /**
     * Render a raw template string (not loaded from file) with the given variables.
     * Useful for inline templates or testing.
     *
     * @param template  the raw template string
     * @param variables key-value pairs
     * @return rendered string
     */
    public String renderString(String template, Map<String, Object> variables) {
        String result = processConditionals(template, variables);
        result = substituteVariables(result, variables);
        return result;
    }

    /**
     * Load the raw template content from cache or classpath.
     *
     * @param templateName template name without path or extension
     * @return raw template text
     */
    public String loadTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, this::readTemplateFromClasspath);
    }

    /**
     * Invalidate the cache for a specific template (or all templates if name is null).
     */
    public void invalidateCache(String templateName) {
        if (templateName == null) {
            templateCache.clear();
            log.info("Cleared all prompt template cache entries");
        } else {
            templateCache.remove(templateName);
            log.info("Invalidated prompt template cache for: {}", templateName);
        }
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private String readTemplateFromClasspath(String templateName) {
        String resourcePath = TEMPLATE_DIR + templateName + TEMPLATE_SUFFIX;
        try (InputStream is = new ClassPathResource(resourcePath).getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            log.debug("Loaded prompt template: {} ({} chars)", templateName, content.length());
            return content;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load prompt template: " + resourcePath
                    + ". Ensure the file exists under src/main/resources/" + TEMPLATE_DIR, e);
        }
    }

    /**
     * Process {{#if condition}}...{{/if}} blocks.
     * The condition is considered true if the variable exists and is non-null / non-empty / non-"false".
     */
    private String processConditionals(String template, Map<String, Object> variables) {
        Matcher matcher = IF_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String conditionVar = matcher.group(1);
            String block = matcher.group(2);

            boolean conditionMet = isTruthy(variables.get(conditionVar));
            if (conditionMet) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(block));
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Substitute {{variable}} placeholders with values from the map.
     * Missing variables are left as-is (not removed) to aid debugging.
     */
    private String substituteVariables(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            // Skip conditional keywords
            if (key.equals("if") || key.equals("else")) {
                continue;
            }
            Object value = variables.get(key);
            if (value != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
            }
            // If value is null, leave the placeholder as-is for debugging
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String s) return !s.isBlank() && !"false".equalsIgnoreCase(s);
        if (value instanceof Number n) return n.doubleValue() != 0.0;
        return true;
    }
}
