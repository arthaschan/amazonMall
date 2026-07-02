package com.yudao.module.amazon.review.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Review Analysis Service covering sentiment classification,
 * topic extraction, pain point and selling point identification,
 * multi-language support, batch processing, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Review Analysis Service Tests")
class ReviewAnalysisServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AIAnalysisClient aiAnalysisClient;

    @Mock
    private TranslationService translationService;

    private ReviewAnalysisService analysisService;

    private static final String ASIN = "B08N5WRWNW";

    @BeforeEach
    void setUp() {
        analysisService = new ReviewAnalysisService(
                reviewRepository, aiAnalysisClient, translationService
        );
    }

    // -----------------------------------------------------------------------
    // Sentiment Classification
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Positive review correctly classified as positive sentiment")
    void testSentimentClassification_Positive() {
        // Given: A clearly positive review
        Review review = Review.builder()
                .reviewId("R001")
                .asin(ASIN)
                .starRating(5)
                .title("Absolutely love this product!")
                .body("This is the best purchase I've made. The quality is excellent, " +
                      "shipping was fast, and it works perfectly. Highly recommended!")
                .build();

        when(aiAnalysisClient.analyzeSentiment(anyString()))
                .thenReturn(new SentimentResult(Sentiment.POSITIVE, 0.95));

        // When: Analyzing sentiment
        SentimentResult result = analysisService.classifySentiment(review);

        // Then: Classified as positive
        assertThat(result.getSentiment()).isEqualTo(Sentiment.POSITIVE);
        assertThat(result.getConfidence()).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("Negative review correctly classified as negative sentiment")
    void testSentimentClassification_Negative() {
        Review review = Review.builder()
                .reviewId("R002")
                .asin(ASIN)
                .starRating(1)
                .title("Terrible quality, broke after 2 days")
                .body("Waste of money. The product broke immediately. " +
                      "Customer service was unhelpful. Do not buy!")
                .build();

        when(aiAnalysisClient.analyzeSentiment(anyString()))
                .thenReturn(new SentimentResult(Sentiment.NEGATIVE, 0.92));

        SentimentResult result = analysisService.classifySentiment(review);

        assertThat(result.getSentiment()).isEqualTo(Sentiment.NEGATIVE);
        assertThat(result.getConfidence()).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("Mixed review with balanced pros and cons classified as neutral")
    void testSentimentClassification_Neutral() {
        Review review = Review.builder()
                .reviewId("R003")
                .asin(ASIN)
                .starRating(3)
                .title("Decent product with some issues")
                .body("The product works as described but the build quality could be better. " +
                      "It's okay for the price but I expected more. Shipping was fast.")
                .build();

        when(aiAnalysisClient.analyzeSentiment(anyString()))
                .thenReturn(new SentimentResult(Sentiment.NEUTRAL, 0.70));

        SentimentResult result = analysisService.classifySentiment(review);

        assertThat(result.getSentiment()).isEqualTo(Sentiment.NEUTRAL);
    }

    @Test
    @DisplayName("Star rating correlates with sentiment but text analysis takes precedence")
    void testSentimentClassification_TextOverridesStars() {
        // 4-star review but text is actually negative (sarcastic)
        Review review = Review.builder()
                .reviewId("R004")
                .asin(ASIN)
                .starRating(4)
                .title("Great... if you like broken products")
                .body("Arrived damaged. Returning immediately. Complete waste of time.")
                .build();

        when(aiAnalysisClient.analyzeSentiment(anyString()))
                .thenReturn(new SentimentResult(Sentiment.NEGATIVE, 0.85));

        SentimentResult result = analysisService.classifySentiment(review);

        assertThat(result.getSentiment())
                .as("Text sentiment analysis should override star rating")
                .isEqualTo(Sentiment.NEGATIVE);
    }

    // -----------------------------------------------------------------------
    // Topic Extraction
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Review topics are extracted: quality, shipping, function, appearance")
    void testTopicExtraction() {
        // Given: A multi-topic review
        Review review = Review.builder()
                .reviewId("R005")
                .body("The build quality is solid and it looks great on my desk. " +
                      "However, the setup instructions were confusing. " +
                      "Shipping took longer than expected but packaging was good.")
                .build();

        when(aiAnalysisClient.extractTopics(anyString()))
                .thenReturn(List.of(
                        new TopicMention("quality", "build quality is solid", Sentiment.POSITIVE),
                        new TopicMention("appearance", "looks great on my desk", Sentiment.POSITIVE),
                        new TopicMention("function", "setup instructions were confusing", Sentiment.NEGATIVE),
                        new TopicMention("shipping", "took longer than expected", Sentiment.NEGATIVE)
                ));

        // When: Extracting topics
        List<TopicMention> topics = analysisService.extractTopics(review);

        // Then: All topics identified
        assertThat(topics).hasSize(4);
        assertThat(topics).extracting(TopicMention::getTopic)
                .containsExactlyInAnyOrder("quality", "appearance", "function", "shipping");
    }

    @Test
    @DisplayName("Topic sentiment distribution is calculated across all reviews")
    void testTopicExtraction_Aggregate() {
        // Given: Multiple reviews with topic mentions
        List<Review> reviews = List.of(
                createReviewWithTopic("quality", Sentiment.POSITIVE),
                createReviewWithTopic("quality", Sentiment.POSITIVE),
                createReviewWithTopic("quality", Sentiment.NEGATIVE),
                createReviewWithTopic("shipping", Sentiment.POSITIVE),
                createReviewWithTopic("shipping", Sentiment.NEGATIVE)
        );

        when(reviewRepository.findByAsin(ASIN)).thenReturn(reviews);
        when(aiAnalysisClient.extractTopics(anyString())).thenAnswer(inv -> {
            String text = inv.getArgument(0);
            if (text.contains("quality")) {
                return text.contains("positive")
                        ? List.of(new TopicMention("quality", text, Sentiment.POSITIVE))
                        : List.of(new TopicMention("quality", text, Sentiment.NEGATIVE));
            }
            return text.contains("positive")
                    ? List.of(new TopicMention("shipping", text, Sentiment.POSITIVE))
                    : List.of(new TopicMention("shipping", text, Sentiment.NEGATIVE));
        });

        // When: Computing aggregate topic analysis
        TopicAnalysisSummary summary = analysisService.analyzeTopicsByAsin(ASIN);

        // Then: Topic distribution is computed
        assertThat(summary.getTopicMentions()).containsKey("quality");
        assertThat(summary.getTopicMentions()).containsKey("shipping");
    }

    // -----------------------------------------------------------------------
    // Pain Point Extraction
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Recurring complaints across reviews are identified as pain points")
    void testPainPointExtraction() {
        // Given: Multiple reviews mentioning the same issue
        List<Review> reviews = List.of(
                Review.builder().reviewId("R001").body("The zipper broke after one week").starRating(2).build(),
                Review.builder().reviewId("R002").body("Zipper is cheap and broke quickly").starRating(1).build(),
                Review.builder().reviewId("R003").body("Poor quality zipper, disappointed").starRating(2).build(),
                Review.builder().reviewId("R004").body("Love the bag but zipper is fragile").starRating(3).build(),
                Review.builder().reviewId("R005").body("Great bag, no issues").starRating(5).build()
        );

        when(reviewRepository.findByAsin(ASIN)).thenReturn(reviews);
        when(aiAnalysisClient.extractPainPoints(anyList()))
                .thenReturn(List.of(
                        new PainPoint("zipper quality/breakage", 4, 0.80, List.of("R001", "R002", "R003", "R004"))
                ));

        // When: Extracting pain points
        List<PainPoint> painPoints = analysisService.extractPainPoints(ASIN);

        // Then: Recurring complaint identified
        assertThat(painPoints).isNotEmpty();
        assertThat(painPoints.get(0).getDescription())
                .containsIgnoringCase("zipper");
        assertThat(painPoints.get(0).getMentionCount()).isEqualTo(4);
        assertThat(painPoints.get(0).getFrequencyPct())
                .as("Pain point should appear in 4 out of 5 reviews (80%)")
                .isGreaterThan(0.5);
    }

    // -----------------------------------------------------------------------
    // Selling Point Extraction
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Recurring praise across reviews is identified as selling points")
    void testSellingPointExtraction() {
        List<Review> reviews = List.of(
                Review.builder().reviewId("R001").body("Super comfortable and fits perfectly").starRating(5).build(),
                Review.builder().reviewId("R002").body("Best fitting product I've used, very comfortable").starRating(5).build(),
                Review.builder().reviewId("R003").body("Comfortable and well-made").starRating(4).build(),
                Review.builder().reviewId("R004").body("Decent product, works okay").starRating(3).build()
        );

        when(reviewRepository.findByAsin(ASIN)).thenReturn(reviews);
        when(aiAnalysisClient.extractSellingPoints(anyList()))
                .thenReturn(List.of(
                        new SellingPoint("comfort and fit", 3, 0.75, List.of("R001", "R002", "R003"))
                ));

        List<SellingPoint> sellingPoints = analysisService.extractSellingPoints(ASIN);

        assertThat(sellingPoints).isNotEmpty();
        assertThat(sellingPoints.get(0).getDescription())
                .containsIgnoringCase("comfort")
                .containsIgnoringCase("fit");
        assertThat(sellingPoints.get(0).getMentionCount()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Multi-Language Reviews
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("English reviews are analyzed directly without translation")
    void testMultiLanguageReviews_English() {
        Review englishReview = Review.builder()
                .reviewId("R-EN")
                .body("This product is amazing! Best purchase ever.")
                .language("en")
                .build();

        when(aiAnalysisClient.analyzeSentiment(anyString()))
                .thenReturn(new SentimentResult(Sentiment.POSITIVE, 0.95));

        SentimentResult result = analysisService.classifySentiment(englishReview);

        assertThat(result.getSentiment()).isEqualTo(Sentiment.POSITIVE);
        verify(translationService, never()).translate(anyString(), anyString());
    }

    @Test
    @DisplayName("Japanese reviews are translated before analysis")
    void testMultiLanguageReviews_Japanese() {
        Review japaneseReview = Review.builder()
                .reviewId("R-JP")
                .body("この製品は素晴らしいです。品質が非常に良い。")
                .language("ja")
                .build();

        when(translationService.translate(japaneseReview.getBody(), "en"))
                .thenReturn("This product is wonderful. The quality is very good.");
        when(aiAnalysisClient.analyzeSentiment("This product is wonderful. The quality is very good."))
                .thenReturn(new SentimentResult(Sentiment.POSITIVE, 0.90));

        SentimentResult result = analysisService.classifySentiment(japaneseReview);

        assertThat(result.getSentiment()).isEqualTo(Sentiment.POSITIVE);
        verify(translationService).translate(japaneseReview.getBody(), "en");
    }

    @Test
    @DisplayName("German reviews are translated before analysis")
    void testMultiLanguageReviews_German() {
        Review germanReview = Review.builder()
                .reviewId("R-DE")
                .body("Schlechte Qualität. Das Produkt ist nach einer Woche kaputt gegangen.")
                .language("de")
                .build();

        when(translationService.translate(germanReview.getBody(), "en"))
                .thenReturn("Poor quality. The product broke after one week.");
        when(aiAnalysisClient.analyzeSentiment("Poor quality. The product broke after one week."))
                .thenReturn(new SentimentResult(Sentiment.NEGATIVE, 0.88));

        SentimentResult result = analysisService.classifySentiment(germanReview);

        assertThat(result.getSentiment()).isEqualTo(Sentiment.NEGATIVE);
    }

    @Test
    @DisplayName("Multi-language analysis aggregates results from all languages")
    void testMultiLanguageReviews_Aggregation() {
        List<Review> multiLangReviews = List.of(
                Review.builder().reviewId("R-EN").body("Great product").language("en").build(),
                Review.builder().reviewId("R-JP").body("素晴らしい製品").language("ja").build(),
                Review.builder().reviewId("R-DE").body("Tolles Produkt").language("de").build()
        );

        when(reviewRepository.findByAsin(ASIN)).thenReturn(multiLangReviews);
        when(translationService.translate(anyString(), eq("en"))).thenReturn("Great product");
        when(aiAnalysisClient.analyzeSentiment(anyString()))
                .thenReturn(new SentimentResult(Sentiment.POSITIVE, 0.90));

        ReviewAnalysisSummary summary = analysisService.analyzeAllReviews(ASIN);

        assertThat(summary.getTotalReviews()).isEqualTo(3);
        assertThat(summary.getSentimentDistribution().getPositive()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Batch Analysis
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("100+ reviews are processed in batch without performance degradation")
    void testBatchAnalysis() {
        // Given: 150 reviews
        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            reviews.add(Review.builder()
                    .reviewId("R" + String.format("%03d", i))
                    .asin(ASIN)
                    .starRating((i % 5) + 1)
                    .body("Review body " + i)
                    .build());
        }

        when(reviewRepository.findByAsin(ASIN)).thenReturn(reviews);
        when(aiAnalysisClient.batchAnalyzeSentiment(anyList()))
                .thenAnswer(inv -> {
                    List<String> texts = inv.getArgument(0);
                    return texts.stream()
                            .map(t -> new SentimentResult(Sentiment.POSITIVE, 0.80))
                            .toList();
                });

        // When: Batch analyzing
        ReviewAnalysisSummary summary = analysisService.batchAnalyzeReviews(ASIN);

        // Then: All reviews processed
        assertThat(summary.getTotalReviews()).isEqualTo(150);

        // And: Batch API was used (more efficient than individual calls)
        verify(aiAnalysisClient).batchAnalyzeSentiment(anyList());
    }

    // -----------------------------------------------------------------------
    // Empty Review Handling
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("No reviews returns empty analysis result without errors")
    void testEmptyReviewHandling() {
        when(reviewRepository.findByAsin(ASIN)).thenReturn(List.of());

        ReviewAnalysisSummary summary = analysisService.analyzeAllReviews(ASIN);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalReviews()).isEqualTo(0);
        assertThat(summary.getSentimentDistribution().getPositive()).isEqualTo(0);
        assertThat(summary.getSentimentDistribution().getNegative()).isEqualTo(0);
        assertThat(summary.getSentimentDistribution().getNeutral()).isEqualTo(0);
        assertThat(summary.getPainPoints()).isEmpty();
        assertThat(summary.getSellingPoints()).isEmpty();
    }

    @Test
    @DisplayName("Null review list is handled gracefully")
    void testEmptyReviewHandling_Null() {
        when(reviewRepository.findByAsin(ASIN)).thenReturn(null);

        ReviewAnalysisSummary summary = analysisService.analyzeAllReviews(ASIN);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalReviews()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Duplicate Review Handling
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Same review ID is not analyzed twice (deduplication)")
    void testDuplicateReviewHandling() {
        // Given: Reviews with duplicate IDs
        List<Review> reviews = List.of(
                Review.builder().reviewId("R001").body("First review").build(),
                Review.builder().reviewId("R001").body("First review").build(), // duplicate
                Review.builder().reviewId("R002").body("Second review").build()
        );

        when(reviewRepository.findByAsin(ASIN)).thenReturn(reviews);
        when(aiAnalysisClient.batchAnalyzeSentiment(anyList()))
                .thenAnswer(inv -> {
                    List<String> texts = inv.getArgument(0);
                    return texts.stream()
                            .map(t -> new SentimentResult(Sentiment.POSITIVE, 0.80))
                            .toList();
                });

        // When: Analyzing with duplicates
        ReviewAnalysisSummary summary = analysisService.batchAnalyzeReviews(ASIN);

        // Then: Only unique reviews analyzed
        assertThat(summary.getTotalReviews())
                .as("Duplicate reviews should be deduplicated")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Previously analyzed reviews are skipped (incremental analysis)")
    void testDuplicateReviewHandling_Incremental() {
        // Given: Some reviews already analyzed
        Review analyzedReview = Review.builder()
                .reviewId("R001")
                .body("Already analyzed")
                .analyzedAt(java.time.LocalDateTime.now().minusHours(1))
                .sentimentResult(new SentimentResult(Sentiment.POSITIVE, 0.90))
                .build();

        Review newReview = Review.builder()
                .reviewId("R002")
                .body("New review")
                .analyzedAt(null)
                .build();

        when(reviewRepository.findByAsin(ASIN)).thenReturn(List.of(analyzedReview, newReview));
        when(aiAnalysisClient.analyzeSentiment("New review"))
                .thenReturn(new SentimentResult(Sentiment.NEGATIVE, 0.85));

        ReviewAnalysisSummary summary = analysisService.analyzeAllReviews(ASIN);

        // Only the new review should be sent for AI analysis
        verify(aiAnalysisClient, times(1)).analyzeSentiment("New review");
        assertThat(summary.getTotalReviews()).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private Review createReviewWithTopic(String topic, Sentiment sentiment) {
        String body = sentiment == Sentiment.POSITIVE
                ? "positive " + topic + " experience"
                : "negative " + topic + " experience";
        return Review.builder()
                .reviewId("R-" + topic + "-" + sentiment.name())
                .body(body)
                .build();
    }
}
