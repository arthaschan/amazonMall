package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;

/**
 * Function tool: Generate an optimized product listing.
 *
 * <p>Loads product data from amazon_product and review insights from amazon_review
 * to assemble the data needed for listing generation. The actual AI-powered text
 * generation requires the Spring AI ChatClient pipeline.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GenerateListingFunction implements Function<GenerateListingFunction.Request, GenerateListingFunction.Response> {

    @Resource
    private AmazonProductMapper amazonProductMapper;

    @Resource
    private AmazonReviewMapper amazonReviewMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** ASIN for existing product, or null for new product */
        private String asin;
        /** Product type/category for new products */
        private String productType;
        /** Target keywords to optimize for */
        private List<String> targetKeywords;
        /** Target language code, e.g. "en" */
        private String language;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String title;
        private List<String> bulletPoints;
        private String description;
        private String backendKeywords;
        private String validationStatus;
        private Double keywordCoveragePct;
    }

    @Override
    public Response apply(Request request) {
        log.info("generateListing called: asin={}, productType={}, keywords={}, language={}",
                request.getAsin(), request.getProductType(),
                request.getTargetKeywords(), request.getLanguage());

        String asin = request.getAsin();
        String language = request.getLanguage() != null ? request.getLanguage() : "en";
        List<String> targetKeywords = request.getTargetKeywords() != null
                ? request.getTargetKeywords() : Collections.<String>emptyList();

        // Step 1: Load product data from DB
        AmazonProductDO product = null;
        if (asin != null && !asin.isEmpty()) {
            try {
                List<AmazonProductDO> products = amazonProductMapper.selectList(
                        new LambdaQueryWrapperX<AmazonProductDO>()
                                .eq(AmazonProductDO::getAsin, asin)
                                .last("LIMIT 1"));
                if (!products.isEmpty()) {
                    product = products.get(0);
                }
            } catch (Exception e) {
                log.error("Failed to load product for ASIN {}: {}", asin, e.getMessage(), e);
            }
        }

        if (product == null && (asin == null || asin.isEmpty())) {
            // New product scenario - no existing data
            return buildNewProductResponse(request.getProductType(), targetKeywords, language);
        }

        if (product == null) {
            return Response.builder()
                    .title("Product not found")
                    .bulletPoints(Collections.singletonList(
                            "No product found for ASIN " + asin + ". Please verify the ASIN and ensure product data has been synced."))
                    .validationStatus("ERROR")
                    .keywordCoveragePct(0.0)
                    .build();
        }

        // Step 2: Load reviews and aggregate insights
        List<String> topSellingPoints = new ArrayList<String>();
        List<String> topPainPoints = new ArrayList<String>();
        try {
            List<AmazonReviewDO> reviews = amazonReviewMapper.selectList(
                    new LambdaQueryWrapperX<AmazonReviewDO>()
                            .eq(AmazonReviewDO::getAsin, asin));

            topSellingPoints = aggregateTopItems(reviews, true);
            topPainPoints = aggregateTopItems(reviews, false);
        } catch (Exception e) {
            log.warn("Failed to load reviews for ASIN {}: {}", asin, e.getMessage());
        }

        // Step 3: Build the response with current listing data + review insights
        String currentTitle = product.getTitle() != null ? product.getTitle() : "";
        List<String> currentBullets = product.getBulletPoints() != null
                ? product.getBulletPoints() : Collections.<String>emptyList();
        String currentDescription = product.getDescription() != null ? product.getDescription() : "";
        String currentBackendKw = product.getBackendKeywords() != null ? product.getBackendKeywords() : "";

        // Build enhanced title incorporating review insights
        StringBuilder titleBuilder = new StringBuilder();
        if (!currentTitle.isEmpty()) {
            titleBuilder.append("[CURRENT] ").append(currentTitle);
        } else {
            String brand = product.getBrand() != null ? product.getBrand() : "";
            titleBuilder.append("[TO BE GENERATED] Brand: ").append(brand)
                    .append(", Category: ").append(product.getCategoryId() != null ? product.getCategoryId() : request.getProductType());
        }

        // Build enhanced bullet points
        List<String> enhancedBullets = new ArrayList<String>();
        if (!currentBullets.isEmpty()) {
            for (String bullet : currentBullets) {
                enhancedBullets.add("[CURRENT] " + bullet);
            }
        }

        // Add review insights as additional context bullets
        if (!topSellingPoints.isEmpty()) {
            enhancedBullets.add("[REVIEW INSIGHT - Selling Points] " + joinStrings(topSellingPoints, "; "));
        }
        if (!topPainPoints.isEmpty()) {
            enhancedBullets.add("[REVIEW INSIGHT - Pain Points to Address] " + joinStrings(topPainPoints, "; "));
        }

        // Add generation note
        enhancedBullets.add("[NOTE] Full AI-powered listing generation requires the Spring AI ChatClient pipeline. "
                + "The data above is assembled from the product catalog and review analysis, "
                + "ready to be sent to an AI model for optimized listing generation.");

        // Keyword coverage analysis against current listing
        double keywordCoverage = calculateKeywordCoverage(currentTitle, currentBullets, currentBackendKw, targetKeywords);

        // Build description with context
        StringBuilder descBuilder = new StringBuilder();
        if (!currentDescription.isEmpty()) {
            descBuilder.append("[CURRENT] ").append(currentDescription);
        }
        if (!topSellingPoints.isEmpty()) {
            descBuilder.append(" | Key selling points from reviews: ").append(joinStrings(topSellingPoints, ", "));
        }

        // Validation status
        String validationStatus = "DATA_ASSEMBLED";
        if (currentTitle.isEmpty()) {
            validationStatus = "MISSING_TITLE";
        } else if (currentBullets.isEmpty()) {
            validationStatus = "MISSING_BULLETS";
        }

        return Response.builder()
                .title(titleBuilder.toString())
                .bulletPoints(enhancedBullets)
                .description(descBuilder.toString())
                .backendKeywords(currentBackendKw.isEmpty()
                        ? "No backend keywords set. Target keywords: " + joinStrings(targetKeywords, ", ")
                        : currentBackendKw)
                .validationStatus(validationStatus)
                .keywordCoveragePct(keywordCoverage)
                .build();
    }

    /**
     * Build a response for a new product (no existing ASIN).
     */
    private Response buildNewProductResponse(String productType, List<String> targetKeywords, String language) {
        List<String> bullets = new ArrayList<String>();
        bullets.add("[NEW PRODUCT] No existing listing data available.");
        bullets.add("Product type: " + (productType != null ? productType : "Not specified"));
        bullets.add("Target keywords: " + (targetKeywords.isEmpty() ? "None specified" : joinStrings(targetKeywords, ", ")));
        bullets.add("Language: " + language);
        bullets.add("[NOTE] Full AI-powered listing generation requires the Spring AI ChatClient pipeline. "
                + "Provide an ASIN to load existing product data and review insights for optimization.");

        return Response.builder()
                .title("[NEW PRODUCT] " + (productType != null ? productType : "New Product"))
                .bulletPoints(bullets)
                .description("No existing description. Listing generation requires AI pipeline integration.")
                .backendKeywords(targetKeywords.isEmpty() ? "" : joinStrings(targetKeywords, " "))
                .validationStatus("NEW_PRODUCT")
                .keywordCoveragePct(0.0)
                .build();
    }

    /**
     * Aggregate top selling points or pain points from reviews.
     * Returns top 5 items sorted by frequency.
     *
     * @param reviews       the review list
     * @param sellingPoints true for selling points, false for pain points
     * @return top items
     */
    private List<String> aggregateTopItems(List<AmazonReviewDO> reviews, boolean sellingPoints) {
        Map<String, Integer> frequency = new LinkedHashMap<String, Integer>();

        for (AmazonReviewDO review : reviews) {
            List<String> items = sellingPoints ? review.getAiSellingPoints() : review.getAiPainPoints();
            if (items != null) {
                for (String item : items) {
                    if (item != null && !item.trim().isEmpty()) {
                        String normalized = item.trim();
                        Integer count = frequency.get(normalized);
                        frequency.put(normalized, count != null ? count + 1 : 1);
                    }
                }
            }
        }

        // Sort by frequency descending
        List<Map.Entry<String, Integer>> sorted = new ArrayList<Map.Entry<String, Integer>>(frequency.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        List<String> result = new ArrayList<String>();
        int limit = Math.min(5, sorted.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            result.add(entry.getKey() + " (" + entry.getValue() + " mentions)");
        }
        return result;
    }

    /**
     * Calculate keyword coverage percentage against the current listing text.
     */
    private double calculateKeywordCoverage(String title, List<String> bullets, String backendKeywords,
                                            List<String> targetKeywords) {
        if (targetKeywords == null || targetKeywords.isEmpty()) {
            return 0.0;
        }

        String allText = ((title != null ? title : "") + " "
                + (bullets != null ? joinStrings(bullets, " ") : "") + " "
                + (backendKeywords != null ? backendKeywords : "")).toLowerCase();

        int matched = 0;
        for (String keyword : targetKeywords) {
            if (keyword != null && allText.contains(keyword.toLowerCase().trim())) {
                matched++;
            }
        }

        return (matched * 100.0) / targetKeywords.size();
    }

    /**
     * Join a list of strings with a delimiter (JDK 8 compatible).
     */
    private static String joinStrings(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }
}
