package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.Function;

/**
 * Function tool: Run AI analysis on product reviews.
 *
 * <p>Queries the amazon_review table directly and computes analysis from
 * pre-computed AI fields (ai_sentiment, ai_pain_points, ai_selling_points, ai_topics).
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class AnalyzeReviewsFunction implements Function<AnalyzeReviewsFunction.Request, AnalyzeReviewsFunction.Response> {

    @Resource
    private AmazonReviewMapper amazonReviewMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String asin;
        /** Analysis type: "sentiment", "pain_points", "selling_points", "personas" */
        private String analysisType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String asin;
        private String analysisType;
        private String result;
        private Integer reviewsAnalyzed;
    }

    @Override
    public Response apply(Request request) {
        log.info("analyzeReviews called: asin={}, analysisType={}", request.getAsin(), request.getAnalysisType());

        String analysisType = request.getAnalysisType() != null ? request.getAnalysisType() : "sentiment";

        // Query all reviews for the given ASIN
        List<AmazonReviewDO> reviews;
        try {
            reviews = amazonReviewMapper.selectList(new LambdaQueryWrapperX<AmazonReviewDO>()
                    .eq(AmazonReviewDO::getAsin, request.getAsin()));
        } catch (Exception e) {
            log.error("Failed to query reviews for ASIN {}: {}", request.getAsin(), e.getMessage(), e);
            return Response.builder()
                    .asin(request.getAsin())
                    .analysisType(analysisType)
                    .result("Error querying reviews: " + e.getMessage())
                    .reviewsAnalyzed(0)
                    .build();
        }

        int totalCount = reviews.size();
        if (totalCount == 0) {
            return Response.builder()
                    .asin(request.getAsin())
                    .analysisType(analysisType)
                    .result("No reviews found for ASIN " + request.getAsin())
                    .reviewsAnalyzed(0)
                    .build();
        }

        String result;
        if ("sentiment".equals(analysisType)) {
            result = buildSentimentAnalysis(reviews);
        } else if ("pain_points".equals(analysisType)) {
            result = buildPainPointsAnalysis(reviews);
        } else if ("selling_points".equals(analysisType)) {
            result = buildSellingPointsAnalysis(reviews);
        } else if ("personas".equals(analysisType)) {
            result = buildPersonasAnalysis(reviews);
        } else {
            result = "Analysis type not recognized. Use: sentiment, pain_points, selling_points, or personas.";
        }

        return Response.builder()
                .asin(request.getAsin())
                .analysisType(analysisType)
                .result(result)
                .reviewsAnalyzed(totalCount)
                .build();
    }

    /**
     * Build sentiment analysis from review ratings and ai_sentiment field.
     */
    private String buildSentimentAnalysis(List<AmazonReviewDO> reviews) {
        int totalCount = reviews.size();

        // Count by rating (1-5 stars)
        Map<Integer, Integer> ratingCounts = new LinkedHashMap<Integer, Integer>();
        for (int i = 1; i <= 5; i++) {
            ratingCounts.put(i, 0);
        }
        for (AmazonReviewDO review : reviews) {
            if (review.getRating() != null && review.getRating() >= 1 && review.getRating() <= 5) {
                ratingCounts.put(review.getRating(), ratingCounts.get(review.getRating()) + 1);
            }
        }

        // Count by AI sentiment
        int positiveCount = 0;
        int neutralCount = 0;
        int negativeCount = 0;
        for (AmazonReviewDO review : reviews) {
            String sentiment = review.getAiSentiment();
            if ("POSITIVE".equalsIgnoreCase(sentiment)) {
                positiveCount++;
            } else if ("NEGATIVE".equalsIgnoreCase(sentiment)) {
                negativeCount++;
            } else {
                neutralCount++;
            }
        }

        double positivePct = totalCount > 0 ? (positiveCount * 100.0 / totalCount) : 0;
        double neutralPct = totalCount > 0 ? (neutralCount * 100.0 / totalCount) : 0;
        double negativePct = totalCount > 0 ? (negativeCount * 100.0 / totalCount) : 0;

        // Compute overall sentiment score (0-100): positive reviews boost, negative reduce
        int overallScore = (int) Math.round((positivePct * 1.0) + (neutralPct * 0.5));

        // Average rating
        double totalRating = 0;
        int ratingCount = 0;
        for (AmazonReviewDO review : reviews) {
            if (review.getRating() != null) {
                totalRating += review.getRating();
                ratingCount++;
            }
        }
        double avgRating = ratingCount > 0 ? totalRating / ratingCount : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Overall sentiment: %d/100. Average rating: %.1f/5. ", overallScore, avgRating));
        sb.append(String.format("Distribution: %.0f%% positive, %.0f%% neutral, %.0f%% negative. ",
                positivePct, neutralPct, negativePct));
        sb.append("Rating breakdown: ");
        for (int i = 5; i >= 1; i--) {
            sb.append(String.format("%d-star: %d (%.0f%%) ", i, ratingCounts.get(i),
                    totalCount > 0 ? (ratingCounts.get(i) * 100.0 / totalCount) : 0));
            if (i > 1) {
                sb.append("| ");
            }
        }
        return sb.toString();
    }

    /**
     * Build pain points analysis by aggregating ai_pain_points from all reviews.
     */
    private String buildPainPointsAnalysis(List<AmazonReviewDO> reviews) {
        Map<String, Integer> painPointFrequency = new LinkedHashMap<String, Integer>();

        for (AmazonReviewDO review : reviews) {
            List<String> painPoints = review.getAiPainPoints();
            if (painPoints != null) {
                for (String pp : painPoints) {
                    if (pp != null && !pp.trim().isEmpty()) {
                        String normalized = pp.trim();
                        Integer count = painPointFrequency.get(normalized);
                        painPointFrequency.put(normalized, count != null ? count + 1 : 1);
                    }
                }
            }
        }

        if (painPointFrequency.isEmpty()) {
            return "No pain points extracted from reviews yet. AI pain point analysis may not have been run on this product's reviews.";
        }

        // Sort by frequency descending, take top 5
        List<Map.Entry<String, Integer>> sorted = new ArrayList<Map.Entry<String, Integer>>(painPointFrequency.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        StringBuilder sb = new StringBuilder("Top pain points: ");
        int limit = Math.min(5, sorted.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            String severity = entry.getValue() >= 10 ? "HIGH" : (entry.getValue() >= 5 ? "MEDIUM" : "LOW");
            sb.append(String.format("%d. %s (%s severity, %d mentions) ",
                    i + 1, entry.getKey(), severity, entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * Build selling points analysis by aggregating ai_selling_points from all reviews.
     */
    private String buildSellingPointsAnalysis(List<AmazonReviewDO> reviews) {
        Map<String, Integer> sellingPointFrequency = new LinkedHashMap<String, Integer>();

        for (AmazonReviewDO review : reviews) {
            List<String> sellingPoints = review.getAiSellingPoints();
            if (sellingPoints != null) {
                for (String sp : sellingPoints) {
                    if (sp != null && !sp.trim().isEmpty()) {
                        String normalized = sp.trim();
                        Integer count = sellingPointFrequency.get(normalized);
                        sellingPointFrequency.put(normalized, count != null ? count + 1 : 1);
                    }
                }
            }
        }

        if (sellingPointFrequency.isEmpty()) {
            return "No selling points extracted from reviews yet. AI selling point analysis may not have been run on this product's reviews.";
        }

        // Sort by frequency descending, take top 5
        List<Map.Entry<String, Integer>> sorted = new ArrayList<Map.Entry<String, Integer>>(sellingPointFrequency.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        StringBuilder sb = new StringBuilder("Top selling points: ");
        int limit = Math.min(5, sorted.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            String credibility = entry.getValue() >= 10 ? "HIGH" : (entry.getValue() >= 5 ? "MEDIUM" : "LOW");
            sb.append(String.format("%d. %s (%s credibility, %d mentions) ",
                    i + 1, entry.getKey(), credibility, entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * Build persona analysis based on rating distribution and topics.
     */
    private String buildPersonasAnalysis(List<AmazonReviewDO> reviews) {
        int totalCount = reviews.size();

        // Rating distribution
        int highRaters = 0; // 4-5 stars
        int midRaters = 0;  // 3 stars
        int lowRaters = 0;  // 1-2 stars
        for (AmazonReviewDO review : reviews) {
            if (review.getRating() != null) {
                if (review.getRating() >= 4) {
                    highRaters++;
                } else if (review.getRating() == 3) {
                    midRaters++;
                } else {
                    lowRaters++;
                }
            }
        }

        // Aggregate topics
        Map<String, Integer> topicFrequency = new LinkedHashMap<String, Integer>();
        for (AmazonReviewDO review : reviews) {
            List<String> topics = review.getAiTopics();
            if (topics != null) {
                for (String topic : topics) {
                    if (topic != null && !topic.trim().isEmpty()) {
                        String normalized = topic.trim();
                        Integer count = topicFrequency.get(normalized);
                        topicFrequency.put(normalized, count != null ? count + 1 : 1);
                    }
                }
            }
        }

        // Get top topics
        List<Map.Entry<String, Integer>> sortedTopics = new ArrayList<Map.Entry<String, Integer>>(topicFrequency.entrySet());
        Collections.sort(sortedTopics, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        int satisfiedPct = totalCount > 0 ? (highRaters * 100 / totalCount) : 0;
        int neutralPct = totalCount > 0 ? (midRaters * 100 / totalCount) : 0;
        int dissatisfiedPct = totalCount > 0 ? (lowRaters * 100 / totalCount) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Customer personas based on %d reviews: ", totalCount));
        sb.append(String.format("1. Satisfied customers (%d%%) - ", satisfiedPct));
        sb.append("generally positive, likely repeat buyers. ");
        sb.append(String.format("2. Neutral observers (%d%%) - ", neutralPct));
        sb.append("mixed feelings, comparing alternatives. ");
        sb.append(String.format("3. Dissatisfied users (%d%%) - ", dissatisfiedPct));
        sb.append("experiencing issues, potential returns. ");

        if (!sortedTopics.isEmpty()) {
            sb.append("Top discussed topics: ");
            int topicLimit = Math.min(5, sortedTopics.size());
            for (int i = 0; i < topicLimit; i++) {
                Map.Entry<String, Integer> entry = sortedTopics.get(i);
                sb.append(String.format("%s (%d mentions)", entry.getKey(), entry.getValue()));
                if (i < topicLimit - 1) {
                    sb.append(", ");
                }
            }
            sb.append(".");
        }

        return sb.toString();
    }
}
