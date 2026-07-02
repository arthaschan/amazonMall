package cn.iocoder.yudao.module.amazon.ai.listing;

import cn.iocoder.yudao.module.amazon.ai.core.AiModelRouter;
import cn.iocoder.yudao.module.amazon.ai.core.AiResponseParser;
import cn.iocoder.yudao.module.amazon.ai.core.AiTokenTracker;
import cn.iocoder.yudao.module.amazon.ai.core.PromptTemplateEngine;
import cn.iocoder.yudao.module.amazon.ai.core.enums.AiTaskType;
import cn.iocoder.yudao.module.amazon.ai.listing.dto.ListingDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI-powered Amazon listing generation service.
 *
 * <p>Generates optimized product listings using a two-phase approach:
 * <ol>
 *   <li><b>Quick Draft</b> (GPT-4o-mini) - fast initial generation for structure</li>
 *   <li><b>Refinement</b> (GPT-4o) - polish, keyword optimization, compliance check</li>
 * </ol>
 *
 * <p>Post-processing includes:
 * <ul>
 *   <li>Forbidden word detection (Amazon TOS violations)</li>
 *   <li>Title/bullet length validation</li>
 *   <li>Backend keyword byte count validation</li>
 *   <li>Keyword density and coverage analysis</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiListingGeneratorService {

    private static final int TITLE_MAX_CHARS = 200;
    private static final int BULLET_MIN_CHARS = 150;
    private static final int BULLET_MAX_CHARS = 250;
    private static final int BACKEND_MAX_BYTES = 250;
    private static final int BULLET_COUNT = 5;

    private static final String TEMPLATE_NAME = "listing_generator";

    /**
     * Forbidden words/phrases that violate Amazon listing policies.
     */
    private static final List<String> FORBIDDEN_WORDS = List.of(
            "best seller", "best-selling", "#1 rated", "number one",
            "100% guaranteed", "satisfaction guaranteed", "money back guarantee",
            "free shipping", "free gift", "cheapest", "lowest price",
            "risk free", "warranty", "on sale", "discount",
            "fda approved", "eco-friendly" // unless certified
    );

    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            String.join("|", FORBIDDEN_WORDS.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.toList())),
            Pattern.CASE_INSENSITIVE
    );

    private final AiModelRouter modelRouter;
    private final PromptTemplateEngine templateEngine;
    private final AiTokenTracker tokenTracker;
    private final AiResponseParser responseParser;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generate a complete optimized listing for a product.
     *
     * <p>Uses two-phase generation:
     * Phase 1: Quick draft with GPT-4o-mini (fast, cost-effective)
     * Phase 2: Refinement with GPT-4o (polish, keyword optimization)
     *
     * @param productInfo product data and configuration
     * @param keywords    target keywords to optimize for
     * @return the generated and validated listing
     */
    public GeneratedListing generateListing(ProductInfo productInfo, List<String> keywords) {
        log.info("Generating listing for product: category={}, language={}",
                productInfo.getCategory(), productInfo.getTargetLanguage());

        // Phase 1: Quick draft with lightweight model
        String draftResponse = generateDraft(productInfo, keywords);
        log.info("Phase 1 (draft) completed");

        // Phase 2: Refinement with full model
        String refinedResponse = refineListing(draftResponse, productInfo, keywords);
        log.info("Phase 2 (refinement) completed");

        // Parse the refined response
        GeneratedListing listing = responseParser.parse(refinedResponse, GeneratedListing.class);

        // Post-processing: validation
        ValidationResult validation = validateListing(listing);
        listing.setValidation(validation);

        if (!validation.isPassed()) {
            log.warn("Listing validation failed: forbiddenWords={}, titleLength={}, bulletLength={}",
                    validation.getForbiddenWordsFound(),
                    validation.isTitleLengthOk(),
                    validation.isBulletLengthOk());

            // Attempt auto-correction for fixable issues
            listing = autoCorrect(listing, validation);
        }

        // Calculate keyword coverage
        listing.setKeywordCoverage(analyzeKeywordCoverage(listing, keywords));

        log.info("Listing generation complete. Title length={}, bullets={}, backend bytes={}",
                listing.getTitle().length(),
                listing.getBulletPoints().size(),
                listing.getBackendKeywords() != null ? listing.getBackendKeywords().getBytes(StandardCharsets.UTF_8).length : 0);

        return listing;
    }

    /**
     * Generate only a title for the given product.
     */
    public String generateTitle(ProductInfo productInfo, List<String> keywords) {
        Map<String, Object> vars = buildTemplateVariables(productInfo, keywords);
        String promptText = templateEngine.render(TEMPLATE_NAME, vars);

        String systemMsg = "You are an Amazon listing expert. Generate ONLY the product title, nothing else. Max 200 characters.";
        String response = callLlm(AiTaskType.LISTING_GENERATION, promptText, systemMsg);

        // Clean and validate
        String title = response.trim().replaceAll("^\"|\"$", "");
        if (title.length() > TITLE_MAX_CHARS) {
            title = title.substring(0, TITLE_MAX_CHARS).replaceAll("\\s+\\S*$", "");
        }

        // Check forbidden words
        List<String> forbidden = detectForbiddenWords(title);
        if (!forbidden.isEmpty()) {
            log.warn("Forbidden words detected in title: {}. Attempting correction.", forbidden);
            title = removeForbiddenWords(title);
        }

        return title;
    }

    /**
     * Generate only bullet points for the given product.
     */
    public List<String> generateBulletPoints(ProductInfo productInfo, List<String> keywords) {
        Map<String, Object> vars = buildTemplateVariables(productInfo, keywords);
        String promptText = templateEngine.render(TEMPLATE_NAME, vars);

        String systemMsg = "You are an Amazon listing expert. Generate ONLY 5 bullet points, each 200-250 characters. "
                + "Start each with a CAPITALIZED benefit phrase. Return as a JSON array of strings.";
        String response = callLlm(AiTaskType.LISTING_GENERATION, promptText, systemMsg);

        List<String> bullets = responseParser.parse(response,
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});

        // Validate and trim
        return bullets.stream()
                .map(b -> b.length() > BULLET_MAX_CHARS ? b.substring(0, BULLET_MAX_CHARS) : b)
                .limit(BULLET_COUNT)
                .collect(Collectors.toList());
    }

    /**
     * Generate backend search terms that complement the visible listing.
     */
    public String generateBackendKeywords(ProductInfo productInfo, List<String> keywords) {
        Map<String, Object> vars = buildTemplateVariables(productInfo, keywords);
        vars.put("existingTitleKeywords", String.join(", ", productInfo.getExistingTitleKeywords() != null ? productInfo.getExistingTitleKeywords() : List.of()));
        vars.put("existingBulletKeywords", String.join(", ", productInfo.getExistingBulletKeywords() != null ? productInfo.getExistingBulletKeywords() : List.of()));

        String promptText = templateEngine.render(TEMPLATE_NAME, vars);
        String systemMsg = "You are an Amazon SEO expert. Generate ONLY backend keywords (search terms). "
                + "Max 250 bytes. Space-separated, no commas. No duplicates. No brand names. "
                + "Do NOT repeat words already in the title or bullets. Include misspellings and synonyms.";
        String response = callLlm(AiTaskType.LISTING_GENERATION, promptText, systemMsg);

        String backendKeywords = response.trim().replaceAll("[,;]", " ").replaceAll("\\s+", " ");
        // Ensure within byte limit
        byte[] bytes = backendKeywords.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > BACKEND_MAX_BYTES) {
            String trimmed = new String(bytes, 0, BACKEND_MAX_BYTES, StandardCharsets.UTF_8);
            // Remove partial word at the end
            int lastSpace = trimmed.lastIndexOf(' ');
            if (lastSpace > 0) {
                backendKeywords = trimmed.substring(0, lastSpace);
            }
        }

        // Deduplicate
        String[] words = backendKeywords.split("\\s+");
        backendKeywords = Arrays.stream(words)
                .distinct()
                .collect(Collectors.joining(" "));

        return backendKeywords;
    }

    // -----------------------------------------------------------------------
    // Two-phase generation
    // -----------------------------------------------------------------------

    private String generateDraft(ProductInfo productInfo, List<String> keywords) {
        Map<String, Object> vars = buildTemplateVariables(productInfo, keywords);
        String promptText = templateEngine.render(TEMPLATE_NAME, vars);

        // Phase 1 uses lightweight model via override
        ChatClient client = modelRouter.getChatClient(AiTaskType.SIMPLE_CLASSIFICATION); // maps to gpt-4o-mini
        String systemMsg = "Generate a draft Amazon listing. Focus on structure and keyword placement. Return valid JSON.";

        try {
            ChatResponse chatResponse = client.prompt()
                    .system(systemMsg)
                    .user(promptText)
                    .call()
                    .chatResponse();

            String content = chatResponse.getResult().getOutput().getText();

            // Track tokens
            trackUsage(AiTaskType.SIMPLE_CLASSIFICATION, chatResponse);

            return content;
        } catch (Exception e) {
            log.warn("Draft generation failed, falling back to primary model: {}", e.getMessage());
            return callLlm(AiTaskType.LISTING_GENERATION, promptText, systemMsg);
        }
    }

    private String refineListing(String draftResponse, ProductInfo productInfo, List<String> keywords) {
        String refinementPrompt = """
                Review and refine this draft Amazon listing for a %s product.

                DRAFT:
                %s

                TARGET KEYWORDS: %s

                REFINEMENT TASKS:
                1. Ensure all target keywords are naturally embedded
                2. Check title is within 200 characters and starts with brand
                3. Verify each bullet is 200-250 characters with capitalized benefit headers
                4. Ensure backend keywords are within 250 bytes with no duplicates
                5. Remove any forbidden/promotional language
                6. Improve readability and conversion appeal
                7. Add the keyword_coverage_report section

                Return the complete refined listing as JSON.
                """.formatted(productInfo.getCategory(), draftResponse, String.join(", ", keywords));

        return callLlm(AiTaskType.LISTING_GENERATION, refinementPrompt,
                "You are an Amazon listing optimization expert. Refine listings for maximum SEO and conversion.");
    }

    // -----------------------------------------------------------------------
    // Validation & auto-correction
    // -----------------------------------------------------------------------

    ValidationResult validateListing(GeneratedListing listing) {
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();

        // Title length
        int titleLen = listing.getTitle() != null ? listing.getTitle().length() : 0;
        builder.titleCharCount(titleLen);
        builder.titleLengthOk(titleLen > 0 && titleLen <= TITLE_MAX_CHARS);

        // Bullet points
        if (listing.getBulletPoints() != null) {
            List<Integer> bulletLengths = listing.getBulletPoints().stream()
                    .map(String::length)
                    .collect(Collectors.toList());
            builder.bulletCharCounts(bulletLengths);
            boolean allBulletsOk = listing.getBulletPoints().size() == BULLET_COUNT
                    && bulletLengths.stream().allMatch(l -> l >= BULLET_MIN_CHARS && l <= BULLET_MAX_CHARS + 30);
            builder.bulletLengthOk(allBulletsOk);
        } else {
            builder.bulletLengthOk(false);
        }

        // Backend keywords
        if (listing.getBackendKeywords() != null) {
            int byteCount = listing.getBackendKeywords().getBytes(StandardCharsets.UTF_8).length;
            builder.backendByteCount(byteCount);
            builder.backendBytesOk(byteCount <= BACKEND_MAX_BYTES);
        }

        // Forbidden words
        String allText = (listing.getTitle() != null ? listing.getTitle() : "") + " "
                + (listing.getBulletPoints() != null ? String.join(" ", listing.getBulletPoints()) : "");
        List<String> forbidden = detectForbiddenWords(allText);
        builder.forbiddenWordsFound(forbidden);

        builder.passed(builder.build().isTitleLengthOk()
                && builder.build().isBulletLengthOk()
                && builder.build().isBackendBytesOk()
                && forbidden.isEmpty());

        ValidationResult result = builder.build();
        // Recalculate passed
        result.setPassed(result.isTitleLengthOk() && result.isBulletLengthOk()
                && result.isBackendBytesOk() && forbidden.isEmpty());
        return result;
    }

    private GeneratedListing autoCorrect(GeneratedListing listing, ValidationResult validation) {
        // Auto-correct title length
        if (!validation.isTitleLengthOk() && listing.getTitle().length() > TITLE_MAX_CHARS) {
            String title = listing.getTitle().substring(0, TITLE_MAX_CHARS);
            int lastSpace = title.lastIndexOf(' ');
            if (lastSpace > TITLE_MAX_CHARS * 0.8) {
                title = title.substring(0, lastSpace);
            }
            listing.setTitle(title);
        }

        // Auto-remove forbidden words
        if (!validation.getForbiddenWordsFound().isEmpty()) {
            listing.setTitle(removeForbiddenWords(listing.getTitle()));
            if (listing.getBulletPoints() != null) {
                listing.setBulletPoints(listing.getBulletPoints().stream()
                        .map(this::removeForbiddenWords)
                        .collect(Collectors.toList()));
            }
        }

        // Update validation
        listing.setValidation(validateListing(listing));
        return listing;
    }

    // -----------------------------------------------------------------------
    // Keyword analysis
    // -----------------------------------------------------------------------

    private KeywordCoverageReport analyzeKeywordCoverage(GeneratedListing listing, List<String> targetKeywords) {
        String titleLower = listing.getTitle() != null ? listing.getTitle().toLowerCase() : "";
        String bulletsLower = listing.getBulletPoints() != null
                ? String.join(" ", listing.getBulletPoints()).toLowerCase() : "";
        String backendLower = listing.getBackendKeywords() != null
                ? listing.getBackendKeywords().toLowerCase() : "";

        List<String> inTitle = new ArrayList<>();
        List<String> inBullets = new ArrayList<>();
        List<String> inBackendOnly = new ArrayList<>();
        List<String> notUsed = new ArrayList<>();

        for (String keyword : targetKeywords) {
            String kw = keyword.toLowerCase().trim();
            if (titleLower.contains(kw)) {
                inTitle.add(kw);
            } else if (bulletsLower.contains(kw)) {
                inBullets.add(kw);
            } else if (backendLower.contains(kw)) {
                inBackendOnly.add(kw);
            } else {
                notUsed.add(kw);
            }
        }

        double coverage = targetKeywords.isEmpty() ? 0
                : ((inTitle.size() + inBullets.size() + inBackendOnly.size()) * 100.0 / targetKeywords.size());

        return KeywordCoverageReport.builder()
                .keywordsUsedInTitle(inTitle)
                .keywordsUsedInBullets(inBullets)
                .keywordsInBackendOnly(inBackendOnly)
                .keywordsNotUsed(notUsed)
                .coveragePercentage(Math.min(coverage, 100.0))
                .build();
    }

    // -----------------------------------------------------------------------
    // Forbidden word detection
    // -----------------------------------------------------------------------

    List<String> detectForbiddenWords(String text) {
        if (text == null || text.isBlank()) return List.of();
        java.util.regex.Matcher matcher = FORBIDDEN_PATTERN.matcher(text);
        Set<String> found = new LinkedHashSet<>();
        while (matcher.find()) {
            found.add(matcher.group().toLowerCase());
        }
        return new ArrayList<>(found);
    }

    private String removeForbiddenWords(String text) {
        if (text == null) return null;
        String result = FORBIDDEN_PATTERN.matcher(text).replaceAll("");
        // Clean up double spaces
        return result.replaceAll("\\s{2,}", " ").trim();
    }

    // -----------------------------------------------------------------------
    // LLM call helper
    // -----------------------------------------------------------------------

    private String callLlm(AiTaskType taskType, String prompt, String systemMessage) {
        ChatClient client = modelRouter.getChatClient(taskType);

        try {
            ChatResponse response = client.prompt()
                    .system(systemMessage)
                    .user(prompt)
                    .call()
                    .chatResponse();

            trackUsage(taskType, response);

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("LLM call failed for task [{}], attempting fallback: {}", taskType.getCode(), e.getMessage());

            // Fallback
            ChatClient fallback = modelRouter.getFallbackChatClient(taskType);
            ChatResponse response = fallback.prompt()
                    .system(systemMessage)
                    .user(prompt)
                    .call()
                    .chatResponse();

            trackUsage(taskType, response);

            return response.getResult().getOutput().getText();
        }
    }

    private void trackUsage(AiTaskType taskType, ChatResponse response) {
        try {
            var metadata = response.getMetadata();
            if (metadata != null && metadata.getUsage() != null) {
                tokenTracker.track(
                        taskType,
                        modelRouter.resolveModel(taskType),
                        (int) metadata.getUsage().getPromptTokens(),
                        (int) metadata.getUsage().getCompletionTokens()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to track token usage: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildTemplateVariables(ProductInfo info, List<String> keywords) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("category", info.getCategory() != null ? info.getCategory() : info.getProductType());
        vars.put("brand", info.getBrand() != null ? info.getBrand() : "");
        vars.put("sellingPoints", info.getSellingPoints() != null ? String.join("; ", info.getSellingPoints()) : "");
        vars.put("keywords", String.join(", ", keywords));
        vars.put("targetLanguage", info.getTargetLanguage() != null ? info.getTargetLanguage() : "en");
        vars.put("marketplace", info.getMarketplace() != null ? info.getMarketplace() : "US");
        vars.put("competitorListings", info.getCompetitorListings() != null
                ? String.join("\n", info.getCompetitorListings()) : "N/A");
        return vars;
    }
}
