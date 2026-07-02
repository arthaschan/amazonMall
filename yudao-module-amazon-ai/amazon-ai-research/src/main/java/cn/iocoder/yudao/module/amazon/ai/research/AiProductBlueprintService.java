package cn.iocoder.yudao.module.amazon.ai.research;

import cn.iocoder.yudao.module.amazon.ai.core.AiModelRouter;
import cn.iocoder.yudao.module.amazon.ai.core.AiResponseParser;
import cn.iocoder.yudao.module.amazon.ai.core.AiTokenTracker;
import cn.iocoder.yudao.module.amazon.ai.core.PromptTemplateEngine;
import cn.iocoder.yudao.module.amazon.ai.core.enums.AiTaskType;
import cn.iocoder.yudao.module.amazon.ai.research.dto.ResearchDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered product blueprint generation service.
 *
 * <p>Ported from the omniscient project's {@code ProductBlueprintService}.
 * Generates a comprehensive product development blueprint using two-phase analysis:
 * <ol>
 *   <li><b>Screening Phase</b> (GPT-4o-mini) - quick complaint extraction and categorization</li>
 *   <li><b>Deep Analysis Phase</b> (GPT-4o) - full blueprint with manufacturing specs, BOM cost,
 *       quality checkpoints, and risk assessment</li>
 * </ol>
 *
 * <p>Output covers: product concept, target customer, differentiators, manufacturing specs,
 * BOM cost analysis, quality checkpoints, and risk assessment.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProductBlueprintService {

    private static final String TEMPLATE_NAME = "product_blueprint";

    private final AiModelRouter modelRouter;
    private final PromptTemplateEngine templateEngine;
    private final AiTokenTracker tokenTracker;
    private final AiResponseParser responseParser;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generate a comprehensive product blueprint from competitive intelligence.
     *
     * <p>Two-phase approach:
     * Phase 1 (mini model): Extract and categorize complaints across competitors
     * Phase 2 (full model): Build the actionable blueprint with manufacturing specs
     *
     * @param productDetail  the target product details
     * @param competitors    competitor product data
     * @param reviewSummary  summarized review intelligence (pain points, selling points)
     * @param suppliers      available supplier information
     * @return complete product blueprint
     */
    public ProductBlueprint generateBlueprint(ProductDetail productDetail,
                                                List<ProductDetail> competitors,
                                                String reviewSummary,
                                                List<SupplierInfo> suppliers) {
        log.info("Generating product blueprint for: {} (ASIN: {})",
                productDetail.getTitle(), productDetail.getAsin());

        // Phase 1: Screening with lightweight model — complaint extraction
        String complaintAnalysis = extractComplaints(productDetail, competitors, reviewSummary);
        log.info("Phase 1 (complaint extraction) completed");

        // Phase 2: Deep analysis with full model — build the blueprint
        ProductBlueprint blueprint = buildBlueprint(productDetail, competitors,
                reviewSummary, suppliers, complaintAnalysis);
        log.info("Phase 2 (blueprint generation) completed");

        return blueprint;
    }

    /**
     * Generate a simplified blueprint for quick product evaluation.
     * Uses only the full model for a single-pass analysis.
     */
    public ProductBlueprint quickBlueprint(ProductDetail productDetail,
                                            List<ProductDetail> competitors,
                                            String reviewSummary) {
        log.info("Generating quick blueprint for: {}", productDetail.getAsin());

        String competitorData = formatCompetitors(competitors);
        String supplierData = "No supplier data available for quick analysis.";

        Map<String, Object> vars = new HashMap<>();
        vars.put("productTitle", productDetail.getTitle());
        vars.put("asin", productDetail.getAsin() != null ? productDetail.getAsin() : "N/A");
        vars.put("category", productDetail.getCategory());
        vars.put("price", productDetail.getPrice() != null ? "$" + productDetail.getPrice() : "N/A");
        vars.put("rating", productDetail.getRating() != null ? String.valueOf(productDetail.getRating()) : "N/A");
        vars.put("reviewCount", productDetail.getReviewCount() != null ? String.valueOf(productDetail.getReviewCount()) : "N/A");
        vars.put("bsr", productDetail.getBsr() != null ? String.valueOf(productDetail.getBsr()) : "N/A");
        vars.put("competitorData", competitorData);
        vars.put("reviewIntelligence", reviewSummary != null ? reviewSummary : "No review data available.");
        vars.put("supplierData", supplierData);

        String prompt = templateEngine.render(TEMPLATE_NAME, vars);

        String response = callLlm(AiTaskType.COMPLEX_ANALYSIS, prompt,
                "You are a senior product development strategist. Return your blueprint as valid JSON.");

        try {
            return responseParser.parse(response, ProductBlueprint.class);
        } catch (AiResponseParser.AiResponseParseException e) {
            log.error("Failed to parse blueprint response: {}", e.getMessage());
            return emptyBlueprint();
        }
    }

    // -----------------------------------------------------------------------
    // Two-phase analysis
    // -----------------------------------------------------------------------

    private String extractComplaints(ProductDetail productDetail,
                                      List<ProductDetail> competitors,
                                      String reviewSummary) {
        String prompt = """
                Analyze the following competitive review intelligence for the "%s" product category.

                PRODUCT: %s (ASIN: %s)
                PRICE: %s | RATING: %s | REVIEWS: %s

                REVIEW INTELLIGENCE:
                %s

                COMPETITORS:
                %s

                TASK: Extract and categorize all complaints, pain points, and improvement opportunities.
                Group them by category: design_flaws, quality_durability, missing_features, sizing_fit,
                packaging, value_perception, usability.

                For each complaint, assess:
                - Frequency (how many reviews mention it)
                - Severity (critical/high/medium/low)
                - Is it a product defect vs. user error?
                - Is it fixable in manufacturing?
                - Estimated fix complexity

                Also identify:
                - Positive features to keep (what customers love)
                - Unmet needs (what customers want but no competitor offers)
                - Unrealistic expectations (complaints that reflect misunderstanding)

                Return a JSON summary of the complaint analysis.
                """.formatted(
                productDetail.getCategory(),
                productDetail.getTitle(),
                productDetail.getAsin(),
                productDetail.getPrice(),
                productDetail.getRating(),
                productDetail.getReviewCount(),
                reviewSummary != null ? reviewSummary : "No review data available.",
                formatCompetitors(competitors)
        );

        return callLlm(AiTaskType.SENTIMENT_ANALYSIS, prompt,
                "You are an Amazon product analyst. Extract and categorize complaints. Return valid JSON.");
    }

    private ProductBlueprint buildBlueprint(ProductDetail productDetail,
                                             List<ProductDetail> competitors,
                                             String reviewSummary,
                                             List<SupplierInfo> suppliers,
                                             String complaintAnalysis) {
        String competitorData = formatCompetitors(competitors);
        String supplierData = formatSuppliers(suppliers);

        Map<String, Object> vars = new HashMap<>();
        vars.put("productTitle", productDetail.getTitle());
        vars.put("asin", productDetail.getAsin() != null ? productDetail.getAsin() : "N/A");
        vars.put("category", productDetail.getCategory());
        vars.put("price", productDetail.getPrice() != null ? "$" + productDetail.getPrice() : "N/A");
        vars.put("rating", productDetail.getRating() != null ? String.valueOf(productDetail.getRating()) : "N/A");
        vars.put("reviewCount", productDetail.getReviewCount() != null ? String.valueOf(productDetail.getReviewCount()) : "N/A");
        vars.put("bsr", productDetail.getBsr() != null ? String.valueOf(productDetail.getBsr()) : "N/A");
        vars.put("competitorData", competitorData);
        vars.put("reviewIntelligence", reviewSummary != null ? reviewSummary : complaintAnalysis);
        vars.put("supplierData", supplierData);

        String prompt = templateEngine.render(TEMPLATE_NAME, vars);

        // Append the complaint analysis context
        String fullPrompt = prompt + "\n\n=== PREVIOUS COMPLAINT ANALYSIS ===\n" + complaintAnalysis;

        String response = callLlm(AiTaskType.COMPLEX_ANALYSIS, fullPrompt,
                "You are a senior product development strategist and manufacturing consultant. "
                + "Return your complete blueprint as valid JSON.");

        try {
            return responseParser.parse(response, ProductBlueprint.class);
        } catch (AiResponseParser.AiResponseParseException e) {
            log.error("Failed to parse blueprint: {}", e.getMessage());
            return emptyBlueprint();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String formatCompetitors(List<ProductDetail> competitors) {
        if (competitors == null || competitors.isEmpty()) {
            return "No competitor data available.";
        }

        return competitors.stream()
                .limit(15)
                .map(c -> String.format("- ASIN: %s | \"%s\" | $%.2f | %.1f★ | %d reviews | BSR: %s",
                        c.getAsin(), c.getTitle(),
                        c.getPrice() != null ? c.getPrice() : 0.0,
                        c.getRating() != null ? c.getRating() : 0.0,
                        c.getReviewCount() != null ? c.getReviewCount() : 0,
                        c.getBsr() != null ? c.getBsr() : "N/A"))
                .collect(Collectors.joining("\n"));
    }

    private String formatSuppliers(List<SupplierInfo> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return "No supplier data available. Use standard Alibaba/1688 pricing estimates.";
        }

        return suppliers.stream()
                .limit(10)
                .map(s -> String.format("- %s (%s) | MOQ: %.0f | Price: $%.2f | Lead: %d weeks | %s",
                        s.getSupplierName(), s.getLocation(),
                        s.getMinOrderQuantity() != null ? s.getMinOrderQuantity() : 0.0,
                        s.getUnitPriceRange() != null ? s.getUnitPriceRange() : 0.0,
                        s.getLeadTimeWeeks() != null ? s.getLeadTimeWeeks() : 0,
                        s.getManufacturingCapabilities() != null ? s.getManufacturingCapabilities() : "N/A"))
                .collect(Collectors.joining("\n"));
    }

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
            log.error("LLM call failed for [{}], trying fallback: {}", taskType.getCode(), e.getMessage());
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
                tokenTracker.track(taskType,
                        modelRouter.resolveModel(taskType),
                        (int) metadata.getUsage().getPromptTokens(),
                        (int) metadata.getUsage().getCompletionTokens());
            }
        } catch (Exception e) {
            log.warn("Failed to track token usage: {}", e.getMessage());
        }
    }

    private ProductBlueprint emptyBlueprint() {
        return ProductBlueprint.builder()
                .productConcept(ProductConcept.builder()
                        .valueProposition("Insufficient data for analysis")
                        .keyDifferentiators(List.of())
                        .build())
                .targetEconomics(TargetEconomics.builder()
                        .targetPrice(0.0).targetCogs(0.0).targetGrossMarginPct(0.0)
                        .build())
                .manufacturingSpecs(ManufacturingSpecs.builder()
                        .materials(List.of()).assemblyComplexity("unknown")
                        .build())
                .bomCost(BomCost.builder().components(List.of()).build())
                .qualityCheckpoints(List.of())
                .risks(List.of())
                .supplierTalkingPoints(List.of())
                .build();
    }
}
