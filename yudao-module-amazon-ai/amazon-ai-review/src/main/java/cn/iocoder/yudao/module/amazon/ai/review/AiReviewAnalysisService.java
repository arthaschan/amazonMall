package cn.iocoder.yudao.module.amazon.ai.review;

import cn.iocoder.yudao.module.amazon.ai.core.AiModelRouter;
import cn.iocoder.yudao.module.amazon.ai.core.AiResponseParser;
import cn.iocoder.yudao.module.amazon.ai.core.AiTokenTracker;
import cn.iocoder.yudao.module.amazon.ai.core.PromptTemplateEngine;
import cn.iocoder.yudao.module.amazon.ai.core.enums.AiTaskType;
import cn.iocoder.yudao.module.amazon.ai.review.dto.ReviewDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI-powered review analysis service for multi-dimensional consumer insights extraction.
 *
 * <p>Ported from the omniscient project's {@code ReviewAnalyzer} pattern.
 * Analyzes product reviews to extract:
 * <ul>
 *   <li>Sentiment scoring with fake review detection</li>
 *   <li>Topic extraction and clustering</li>
 *   <li>Pain point identification with severity scoring</li>
 *   <li>Selling point extraction with credibility assessment</li>
 *   <li>User persona generation from review evidence</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Batch processing with configurable chunk size (50 reviews per LLM call)</li>
 *   <li>Rate limiting via semaphore to avoid API throttling</li>
 *   <li>Multi-chunk merge for large review sets</li>
 *   <li>Verified purchase weighting</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewAnalysisService {

    private static final String TEMPLATE_NAME = "review_analysis";
    private static final int CHUNK_SIZE = 50;
    private static final int MAX_CONCURRENT_REQUESTS = 3;

    private final AiModelRouter modelRouter;
    private final PromptTemplateEngine templateEngine;
    private final AiTokenTracker tokenTracker;
    private final AiResponseParser responseParser;

    /** Rate limiter: max concurrent LLM requests */
    private final Semaphore rateLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Analyze a batch of reviews and produce a comprehensive analysis report.
     *
     * <p>For large review sets (>50), reviews are chunked and analyzed in parallel,
     * then merged into a consolidated report.
     *
     * @param reviews       the reviews to analyze
     * @param productTitle  the product title for context
     * @param category      the product category
     * @param asin          the product ASIN
     * @return comprehensive analysis report
     */
    public ReviewAnalysisReport analyzeBatch(List<Review> reviews, String productTitle,
                                             String category, String asin) {
        if (reviews == null || reviews.isEmpty()) {
            log.warn("Empty review list provided for analysis");
            return emptyReport();
        }

        log.info("Analyzing {} reviews for ASIN {} ({})", reviews.size(), asin, category);

        // Prioritize verified purchases and negative reviews
        List<Review> sortedReviews = prioritizeReviews(reviews);

        // Chunk into batches
        List<List<Review>> chunks = partition(sortedReviews, CHUNK_SIZE);
        log.info("Split into {} chunks of up to {} reviews", chunks.size(), CHUNK_SIZE);

        if (chunks.size() == 1) {
            // Single chunk: direct analysis
            return analyzeChunk(chunks.get(0), productTitle, category, asin);
        }

        // Multiple chunks: analyze in parallel, then merge
        List<ReviewAnalysisReport> chunkReports = analyzeChunksParallel(
                chunks, productTitle, category, asin);

        return mergeReports(chunkReports, productTitle, reviews.size());
    }

    /**
     * Quick sentiment-only analysis for a small set of reviews.
     * Uses the lightweight model for cost efficiency.
     */
    public SentimentAnalysis quickSentimentAnalysis(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return SentimentAnalysis.builder()
                    .overallScore(50).trend("unknown").fakeReviewRisk(0)
                    .distribution(Map.of("positive_pct", 0.0, "neutral_pct", 0.0, "negative_pct", 0.0))
                    .build();
        }

        String reviewsText = formatReviewsForPrompt(reviews.subList(0, Math.min(reviews.size(), 30)));

        String prompt = """
                Analyze the sentiment of these Amazon product reviews.

                REVIEWS:
                %s

                Return a JSON object with:
                {
                    "overall_score": <0-100>,
                    "distribution": {"positive_pct": <pct>, "neutral_pct": <pct>, "negative_pct": <pct>},
                    "trend": "<improving|stable|declining>",
                    "fake_review_risk": <0-100>
                }
                """.formatted(reviewsText);

        String response = callLlm(AiTaskType.SENTIMENT_ANALYSIS, prompt,
                "You are a sentiment analysis expert. Return only valid JSON.");

        return responseParser.parse(response, SentimentAnalysis.class);
    }

    /**
     * Extract pain points from reviews (focused analysis on negative feedback).
     */
    public List<PainPoint> extractPainPoints(List<Review> reviews, String productTitle) {
        // Focus on negative reviews (1-3 stars)
        List<Review> negativeReviews = reviews.stream()
                .filter(r -> r.getRating() != null && r.getRating() <= 3)
                .sorted(Comparator.comparingInt(r -> r.getRating() != null ? r.getRating() : 5))
                .limit(50)
                .collect(Collectors.toList());

        if (negativeReviews.isEmpty()) {
            log.info("No negative reviews found for pain point analysis");
            return List.of();
        }

        String reviewsText = formatReviewsForPrompt(negativeReviews);
        String prompt = """
                Extract specific, concrete pain points from these negative Amazon reviews for "%s".

                REVIEWS:
                %s

                RULES:
                - Only include pain points with direct evidence (quotes)
                - Distinguish product defects from user error
                - Score severity: critical (primary function fails), high (significantly impairs), medium (annoying), low (cosmetic)
                - Do NOT include shipping/Amazon issues

                Return JSON:
                {
                    "pain_points": [
                        {
                            "issue": "<specific pain point>",
                            "severity": "<critical|high|medium|low>",
                            "frequency": <count>,
                            "is_product_defect": <true|false>,
                            "fixable_in_manufacturing": <true|false>,
                            "sample_quotes": ["<quote>"],
                            "affected_personas": ["<persona>"]
                        }
                    ]
                }
                """.formatted(productTitle, reviewsText);

        String response = callLlm(AiTaskType.SENTIMENT_ANALYSIS, prompt,
                "You are a product quality analyst. Extract actionable pain points. Return only valid JSON.");

        try {
            Map<String, Object> parsed = responseParser.parseToMap(response);
            Object painPointsObj = parsed.get("pain_points");
            if (painPointsObj instanceof List<?> list) {
                return list.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> responseParser.parse(
                                serializeObject(item), PainPoint.class))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to parse pain points response: {}", e.getMessage());
        }

        return List.of();
    }

    // -----------------------------------------------------------------------
    // Chunk analysis
    // -----------------------------------------------------------------------

    private ReviewAnalysisReport analyzeChunk(List<Review> reviews, String productTitle,
                                              String category, String asin) {
        String reviewsText = formatReviewsForPrompt(reviews);

        Map<String, Object> vars = new HashMap<>();
        vars.put("productTitle", productTitle);
        vars.put("category", category);
        vars.put("asin", asin != null ? asin : "N/A");
        vars.put("averageRating", String.format("%.1f",
                reviews.stream().mapToInt(r -> r.getRating() != null ? r.getRating() : 3).average().orElse(3.0)));
        vars.put("totalReviews", String.valueOf(reviews.size()));
        vars.put("reviews", reviewsText);

        String prompt = templateEngine.render(TEMPLATE_NAME, vars);

        String response = callLlm(AiTaskType.SENTIMENT_ANALYSIS, prompt,
                "You are an e-commerce consumer insights analyst. Return your analysis as valid JSON.");

        try {
            ReviewAnalysisReport report = responseParser.parse(response, ReviewAnalysisReport.class);
            report.setTotalReviewsAnalyzed(reviews.size());
            return report;
        } catch (AiResponseParser.AiResponseParseException e) {
            log.error("Failed to parse review analysis response: {}", e.getMessage());
            return emptyReport();
        }
    }

    private List<ReviewAnalysisReport> analyzeChunksParallel(List<List<Review>> chunks,
                                                             String productTitle,
                                                             String category,
                                                             String asin) {
        return chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> {
                    try {
                        rateLimiter.acquire(1);
                        try {
                            return analyzeChunk(chunk, productTitle, category, asin);
                        } finally {
                            rateLimiter.release(1);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Chunk analysis interrupted");
                        return emptyReport();
                    }
                }))
                .collect(Collectors.toList())
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private ReviewAnalysisReport mergeReports(List<ReviewAnalysisReport> reports,
                                              String productTitle, int totalReviews) {
        if (reports.isEmpty()) return emptyReport();
        if (reports.size() == 1) return reports.get(0);

        // Merge pain points
        Map<String, PainPoint> mergedPainPoints = new LinkedHashMap<>();
        for (ReviewAnalysisReport report : reports) {
            if (report.getPainPoints() != null) {
                for (PainPoint pp : report.getPainPoints()) {
                    mergedPainPoints.merge(pp.getIssue(), pp, (existing, incoming) -> {
                        existing.setFrequency(
                                (existing.getFrequency() != null ? existing.getFrequency() : 0)
                                + (incoming.getFrequency() != null ? incoming.getFrequency() : 0));
                        existing.getSampleQuotes().addAll(incoming.getSampleQuotes());
                        return existing;
                    });
                }
            }
        }

        // Merge selling points
        Map<String, SellingPoint> mergedSellingPoints = new LinkedHashMap<>();
        for (ReviewAnalysisReport report : reports) {
            if (report.getSellingPoints() != null) {
                for (SellingPoint sp : report.getSellingPoints()) {
                    mergedSellingPoints.merge(sp.getFeature(), sp, (existing, incoming) -> {
                        existing.setFrequency(
                                (existing.getFrequency() != null ? existing.getFrequency() : 0)
                                + (incoming.getFrequency() != null ? incoming.getFrequency() : 0));
                        return existing;
                    });
                }
            }
        }

        // Merge use cases
        Set<String> allUseCases = new LinkedHashSet<>();
        for (ReviewAnalysisReport report : reports) {
            if (report.getCommonUseCases() != null) {
                allUseCases.addAll(report.getCommonUseCases());
            }
        }

        // Average sentiment
        double avgSentiment = reports.stream()
                .filter(r -> r.getSentiment() != null && r.getSentiment().getOverallScore() != null)
                .mapToInt(r -> r.getSentiment().getOverallScore())
                .average().orElse(50.0);

        SentimentAnalysis mergedSentiment = SentimentAnalysis.builder()
                .overallScore((int) avgSentiment)
                .trend("stable")
                .fakeReviewRisk((int) reports.stream()
                        .filter(r -> r.getSentiment() != null && r.getSentiment().getFakeReviewRisk() != null)
                        .mapToInt(r -> r.getSentiment().getFakeReviewRisk())
                        .average().orElse(0))
                .distribution(Map.of("positive_pct", 0.0, "neutral_pct", 0.0, "negative_pct", 0.0))
                .build();

        return ReviewAnalysisReport.builder()
                .sentiment(mergedSentiment)
                .painPoints(new ArrayList<>(mergedPainPoints.values()))
                .sellingPoints(new ArrayList<>(mergedSellingPoints.values()))
                .commonUseCases(new ArrayList<>(allUseCases))
                .userPersonas(reports.get(0).getUserPersonas()) // Keep from first report
                .topics(reports.get(0).getTopics())
                .totalReviewsAnalyzed(totalReviews)
                .sampleSizeWarning(totalReviews < 20
                        ? "Warning: fewer than 20 reviews analyzed. Conclusions may be unreliable."
                        : null)
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<Review> prioritizeReviews(List<Review> reviews) {
        return reviews.stream()
                .sorted((a, b) -> {
                    // Verified purchases first
                    boolean aVerified = a.getVerifiedPurchase() != null && a.getVerifiedPurchase();
                    boolean bVerified = b.getVerifiedPurchase() != null && b.getVerifiedPurchase();
                    if (aVerified != bVerified) return aVerified ? -1 : 1;

                    // Then by rating (low first for pain point analysis)
                    int aRating = a.getRating() != null ? a.getRating() : 3;
                    int bRating = b.getRating() != null ? b.getRating() : 3;
                    return Integer.compare(aRating, bRating);
                })
                .collect(Collectors.toList());
    }

    private String formatReviewsForPrompt(List<Review> reviews) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reviews.size(); i++) {
            Review r = reviews.get(i);
            sb.append(String.format("[Review %d] Rating: %d/5 | %s | %d helpful votes\n",
                    i + 1,
                    r.getRating() != null ? r.getRating() : 0,
                    (r.getVerifiedPurchase() != null && r.getVerifiedPurchase()) ? "Verified" : "Unverified",
                    r.getHelpfulVotes() != null ? r.getHelpfulVotes() : 0));
            sb.append("Title: ").append(r.getTitle() != null ? r.getTitle() : "No title").append("\n");
            sb.append("Body: ").append(r.getBody() != null ? r.getBody() : "No body").append("\n\n");
        }
        return sb.toString();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
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
            log.error("LLM call failed for task [{}], attempting fallback: {}", taskType.getCode(), e.getMessage());
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

    private String serializeObject(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private ReviewAnalysisReport emptyReport() {
        return ReviewAnalysisReport.builder()
                .sentiment(SentimentAnalysis.builder()
                        .overallScore(50).trend("unknown").fakeReviewRisk(0)
                        .distribution(Map.of("positive_pct", 0.0, "neutral_pct", 0.0, "negative_pct", 0.0))
                        .build())
                .topics(List.of())
                .painPoints(List.of())
                .sellingPoints(List.of())
                .userPersonas(List.of())
                .commonUseCases(List.of())
                .competitorMentions(List.of())
                .improvementOpportunities(List.of())
                .totalReviewsAnalyzed(0)
                .build();
    }
}
