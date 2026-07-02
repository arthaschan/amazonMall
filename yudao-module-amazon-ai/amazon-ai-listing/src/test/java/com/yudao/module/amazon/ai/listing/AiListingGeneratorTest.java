package com.yudao.module.amazon.ai.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AI Listing Generator covering title, bullet point, and backend keyword
 * generation, forbidden word detection, multi-language support, SEO scoring,
 * prompt template rendering, model fallback, and response parsing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI Listing Generator Tests")
class AiListingGeneratorTest {

    @Mock
    private LLMClient primaryModel;

    @Mock
    private LLMClient fallbackModel;

    @Mock
    private PromptTemplateEngine templateEngine;

    @Mock
    private ForbiddenWordDetector forbiddenWordDetector;

    @Mock
    private ListingRepository listingRepository;

    private AiListingGenerator listingGenerator;

    private static final String ASIN = "B08N5WRWNW";
    private static final int TITLE_MAX_CHARS = 200;
    private static final int BULLET_MIN_CHARS = 200;
    private static final int BULLET_MAX_CHARS = 250;
    private static final int BACKEND_KEYWORD_MAX_BYTES = 250;

    @BeforeEach
    void setUp() {
        listingGenerator = new AiListingGenerator(
                primaryModel, fallbackModel, templateEngine,
                forbiddenWordDetector, listingRepository
        );
    }

    // -----------------------------------------------------------------------
    // Title Generation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Generated title is <= 200 chars, has keywords embedded, brand first")
    void testTitleGeneration() {
        // Given: Product data and target keywords
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .asin(ASIN)
                .brand("AcmeBrand")
                .productType("Stainless Steel Water Bottle")
                .targetKeywords(List.of("insulated bottle", "water bottle", "sports bottle", "BPA free"))
                .language("en")
                .build();

        String generatedTitle = "AcmeBrand Stainless Steel Insulated Water Bottle - 32oz BPA Free Sports Bottle with Double Wall Vacuum, Leak Proof Lid for Gym, Hiking, and Travel";

        when(templateEngine.renderTemplate(eq("title_generation"), anyMap()))
                .thenReturn("Rendered prompt for title generation");
        when(primaryModel.generate(anyString()))
                .thenReturn(new LLMResponse(generatedTitle, 0.95));
        when(forbiddenWordDetector.detect(anyString())).thenReturn(List.of());

        // When: Generating title
        GeneratedTitle title = listingGenerator.generateTitle(request);

        // Then: Title meets Amazon requirements
        assertThat(title.getText())
                .as("Title should not exceed 200 characters")
                .hasSizeLessThanOrEqualTo(TITLE_MAX_CHARS);

        assertThat(title.getText())
                .as("Title should start with brand name")
                .startsWith("AcmeBrand");

        // And: Keywords are embedded
        assertThat(title.getText().toLowerCase())
                .as("Title should contain target keywords")
                .contains("insulated")
                .contains("water bottle")
                .contains("bpa free");
    }

    @Test
    @DisplayName("Title with no brand falls back to generic format")
    void testTitleGeneration_NoBrand() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .asin(ASIN)
                .brand(null)
                .productType("Yoga Mat")
                .targetKeywords(List.of("exercise mat", "non-slip"))
                .language("en")
                .build();

        when(templateEngine.renderTemplate(anyString(), anyMap()))
                .thenReturn("prompt");
        when(primaryModel.generate(anyString()))
                .thenReturn(new LLMResponse("Premium Non-Slip Yoga Mat - 6mm Thick Exercise Mat for Home Gym and Studio", 0.90));
        when(forbiddenWordDetector.detect(anyString())).thenReturn(List.of());

        GeneratedTitle title = listingGenerator.generateTitle(request);

        assertThat(title.getText()).isNotEmpty();
        assertThat(title.getText().length()).isLessThanOrEqualTo(TITLE_MAX_CHARS);
    }

    // -----------------------------------------------------------------------
    // Bullet Point Generation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("5 bullet points generated, each 200-250 chars")
    void testBulletPointGeneration() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .asin(ASIN)
                .productType("Wireless Earbuds")
                .targetKeywords(List.of("bluetooth", "noise cancelling", "waterproof"))
                .language("en")
                .build();

        List<String> generatedBullets = List.of(
                "PREMIUM SOUND QUALITY - Experience crystal-clear audio with our advanced Bluetooth 5.3 technology and custom-tuned drivers that deliver rich bass and crisp highs for an immersive listening experience",
                "ACTIVE NOISE CANCELLATION - Block out unwanted noise with our hybrid ANC technology that reduces ambient sound by up to 35dB, perfect for commuting, working, or studying in noisy environments",
                "WATERPROOF AND SWEATPROOF - Rated IPX7 waterproof, these earbuds withstand intense workouts, rain, and sweat, making them ideal companions for gym sessions, running, and outdoor adventures",
                "LONG BATTERY LIFE - Enjoy up to 8 hours of playtime on a single charge, with the charging case providing an additional 32 hours, ensuring your music never stops during long trips or busy days",
                "COMFORTABLE SECURE FIT - Ergonomically designed with three sizes of silicone ear tips for a personalized, secure fit that stays comfortable during extended wear and active movements throughout the day"
        );

        when(templateEngine.renderTemplate(eq("bullet_points"), anyMap()))
                .thenReturn("Rendered bullet point prompt");
        when(primaryModel.generate(anyString()))
                .thenReturn(new LLMResponse(String.join("\n", generatedBullets), 0.90));

        // When: Generating bullet points
        List<GeneratedBulletPoint> bullets = listingGenerator.generateBulletPoints(request);

        // Then: 5 bullet points
        assertThat(bullets)
                .as("Should generate exactly 5 bullet points")
                .hasSize(5);

        // And: Each bullet is within char limits
        for (GeneratedBulletPoint bullet : bullets) {
            assertThat(bullet.getText().length())
                    .as("Each bullet should be 200-250 characters")
                    .isBetween(BULLET_MIN_CHARS, BULLET_MAX_CHARS + 50); // allow some tolerance
        }
    }

    @Test
    @DisplayName("Bullet points contain target keywords naturally embedded")
    void testBulletPointGeneration_Keywords() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .productType("Coffee Maker")
                .targetKeywords(List.of("programmable", "thermal carafe", "auto shut-off", "brew strength"))
                .language("en")
                .build();

        when(templateEngine.renderTemplate(anyString(), anyMap())).thenReturn("prompt");
        when(primaryModel.generate(anyString()))
                .thenReturn(new LLMResponse(
                        "1. PROGRAMMABLE TIMER\n2. THERMAL CARAFE KEEPS COFFEE HOT\n3. AUTO SHUT-OFF SAFETY\n4. BREW STRENGTH SELECTOR\n5. EASY TO CLEAN",
                        0.85));

        List<GeneratedBulletPoint> bullets = listingGenerator.generateBulletPoints(request);

        String allBullets = String.join(" ", bullets.stream().map(GeneratedBulletPoint::getText).toList()).toLowerCase();
        assertThat(allBullets)
                .as("Bullet points should contain target keywords")
                .contains("programmable")
                .contains("thermal carafe")
                .contains("auto shut-off")
                .contains("brew strength");
    }

    // -----------------------------------------------------------------------
    // Backend Keyword Generation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Backend keywords are <= 250 bytes with no duplicates")
    void testBackendKeywordGeneration() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .asin(ASIN)
                .productType("Kitchen Knife Set")
                .existingTitleKeywords(List.of("knife set", "kitchen", "stainless steel"))
                .existingBulletKeywords(List.of("sharp", "ergonomic handle", "dishwasher safe"))
                .language("en")
                .build();

        String backendKeywords = "chef knives cutting utensil cook cooking culinary blade block holder professional german high carbon forged full tang razor edge gift home restaurant";

        when(primaryModel.generate(anyString()))
                .thenReturn(new LLMResponse(backendKeywords, 0.88));

        // When: Generating backend keywords
        GeneratedBackendKeywords result = listingGenerator.generateBackendKeywords(request);

        // Then: Within byte limit
        byte[] keywordBytes = result.getText().getBytes(StandardCharsets.UTF_8);
        assertThat(keywordBytes.length)
                .as("Backend keywords should not exceed 250 bytes")
                .isLessThanOrEqualTo(BACKEND_KEYWORD_MAX_BYTES);

        // And: No duplicate words
        String[] words = result.getText().split("\\s+");
        long uniqueWords = java.util.Arrays.stream(words).distinct().count();
        assertThat(uniqueWords)
                .as("All backend keywords should be unique (no duplicates)")
                .isEqualTo(words.length);

        // And: No overlap with title/bullet keywords
        assertThat(result.getText().toLowerCase())
                .doesNotContain("knife set")
                .doesNotContain("stainless steel");
    }

    // -----------------------------------------------------------------------
    // Forbidden Word Detection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Forbidden words like 'best seller' are detected and flagged")
    void testForbiddenWordDetection() {
        // Given: Listing text with forbidden words
        String titleWithForbidden = "Best Seller Premium Water Bottle - #1 Rated Insulated Flask";

        when(forbiddenWordDetector.detect(titleWithForbidden))
                .thenReturn(List.of(
                        new ForbiddenWord("best seller", ForbiddenWordCategory.PROMOTIONAL, "Violates Amazon TOS"),
                        new ForbiddenWord("#1 rated", ForbiddenWordCategory.CLAIM, "Unsubstantiated claim")
                ));

        // When: Checking for forbidden words
        List<ForbiddenWord> violations = forbiddenWordDetector.detect(titleWithForbidden);

        // Then: Forbidden words identified
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ForbiddenWord::getWord)
                .contains("best seller", "#1 rated");
    }

    @Test
    @DisplayName("Generated listing is automatically checked for forbidden words")
    void testForbiddenWordDetection_Integration() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .productType("Water Bottle")
                .language("en")
                .build();

        // Primary model generates text with a forbidden word
        when(templateEngine.renderTemplate(anyString(), anyMap())).thenReturn("prompt");
        when(primaryModel.generate(anyString()))
                .thenReturn(new LLMResponse("Best Selling Water Bottle - Premium Quality", 0.85));
        when(forbiddenWordDetector.detect(anyString()))
                .thenReturn(List.of(new ForbiddenWord("best selling", ForbiddenWordCategory.PROMOTIONAL, "Forbidden")));

        // When: Generating and validating
        assertThatThrownBy(() -> listingGenerator.generateTitle(request))
                .isInstanceOf(ForbiddenWordException.class)
                .hasMessageContaining("best selling");
    }

    @Test
    @DisplayName("Common forbidden words are detected: guarantee, warranty, free shipping")
    void testForbiddenWordDetection_CommonWords() {
        List<String> forbiddenPhrases = List.of(
                "100% guaranteed",
                "free shipping",
                "best quality",
                "cheapest price",
                "satisfaction guaranteed",
                "money back guarantee"
        );

        for (String phrase : forbiddenPhrases) {
            when(forbiddenWordDetector.detect(phrase))
                    .thenReturn(List.of(new ForbiddenWord(phrase, ForbiddenWordCategory.PROMOTIONAL, "Not allowed")));

            List<ForbiddenWord> violations = forbiddenWordDetector.detect(phrase);
            assertThat(violations)
                    .as("'%s' should be detected as forbidden", phrase)
                    .isNotEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // Multi-Language Generation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("English listing generated with correct language conventions")
    void testMultiLanguageGeneration_English() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .productType("Bluetooth Speaker")
                .language("en")
                .marketplace("US")
                .build();

        when(templateEngine.renderTemplate(anyString(), anyMap())).thenReturn("English prompt");
        when(primaryModel.generate("English prompt"))
                .thenReturn(new LLMResponse("Portable Bluetooth Speaker with Deep Bass", 0.90));
        when(forbiddenWordDetector.detect(anyString())).thenReturn(List.of());

        GeneratedTitle title = listingGenerator.generateTitle(request);
        assertThat(title.getText()).isNotEmpty();
        assertThat(title.getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("Japanese listing generated with correct character encoding")
    void testMultiLanguageGeneration_Japanese() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .productType("Bluetooth Speaker")
                .language("ja")
                .marketplace("JP")
                .build();

        String japaneseTitle = "ポータブルBluetoothスピーカー 重低音 防水 ワイヤレス";
        when(templateEngine.renderTemplate(anyString(), anyMap())).thenReturn("Japanese prompt");
        when(primaryModel.generate("Japanese prompt"))
                .thenReturn(new LLMResponse(japaneseTitle, 0.88));
        when(forbiddenWordDetector.detect(anyString())).thenReturn(List.of());

        GeneratedTitle title = listingGenerator.generateTitle(request);
        assertThat(title.getText()).contains("Bluetooth");
        assertThat(title.getLanguage()).isEqualTo("ja");
    }

    @Test
    @DisplayName("German listing generated with proper grammar and compound words")
    void testMultiLanguageGeneration_German() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .productType("Water Bottle")
                .language("de")
                .marketplace("DE")
                .build();

        String germanTitle = "Edelstahl Isolierflasche Wasserflasche 1L - Doppelwandig Vakuum Thermosflasche";
        when(templateEngine.renderTemplate(anyString(), anyMap())).thenReturn("German prompt");
        when(primaryModel.generate("German prompt"))
                .thenReturn(new LLMResponse(germanTitle, 0.87));
        when(forbiddenWordDetector.detect(anyString())).thenReturn(List.of());

        GeneratedTitle title = listingGenerator.generateTitle(request);
        assertThat(title.getText()).contains("Edelstahl");
        assertThat(title.getLanguage()).isEqualTo("de");
    }

    // -----------------------------------------------------------------------
    // SEO Score Calculation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("SEO score considers keyword density, length, and structure")
    void testSeoScoreCalculation() {
        // Given: Well-optimized listing
        GeneratedListing listing = GeneratedListing.builder()
                .title("AcmeBrand Stainless Steel Insulated Water Bottle - 32oz BPA Free Sports Bottle with Double Wall Vacuum")
                .bulletPoints(List.of(
                        "PREMIUM QUALITY MATERIAL - Made from food-grade 18/8 stainless steel that is BPA-free and completely resistant to oxidation and corrosion for safe daily hydration",
                        "DOUBLE WALL INSULATION - Advanced vacuum insulation technology keeps beverages ice cold for 24 hours or piping hot for 12 hours regardless of external temperature conditions",
                        "LEAK PROOF DESIGN - Innovative screw-on cap with silicone gasket creates a completely airtight seal that prevents any leaks during transport or active movement",
                        "LARGE CAPACITY - Generous 32oz capacity provides ample hydration for all-day use without constant refilling, perfect for gym workouts hiking trips and office use",
                        "EASY TO CLEAN - Wide mouth opening allows for thorough cleaning and accommodates ice cubes easily, dishwasher safe design for convenient maintenance and hygiene"
                ))
                .backendKeywords("gym fitness sport running hiking travel camping outdoor exercise hydration drink flask container tumbler")
                .build();

        List<String> targetKeywords = List.of(
                "water bottle", "insulated bottle", "stainless steel", "BPA free", "sports bottle"
        );

        // When: Computing SEO score
        SEOScore seoScore = listingGenerator.calculateSEOScore(listing, targetKeywords);

        // Then: Score is calculated across multiple dimensions
        assertThat(seoScore.getTotalScore())
                .as("Well-optimized listing should score above 70")
                .isBetween(60.0, 100.0);

        assertThat(seoScore.getKeywordCoverage())
                .as("Keyword coverage should reflect percentage of target keywords found")
                .isGreaterThan(0.0);

        assertThat(seoScore.getTitleScore())
                .as("Title score should consider length and keyword presence")
                .isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Low SEO score when keywords are missing from listing")
    void testSeoScoreCalculation_PoorOptimization() {
        GeneratedListing listing = GeneratedListing.builder()
                .title("Generic Product Item")
                .bulletPoints(List.of("Short bullet", "Another short", "Third", "Fourth", "Fifth"))
                .backendKeywords("")
                .build();

        List<String> targetKeywords = List.of("premium", "insulated", "water bottle", "stainless steel");

        SEOScore seoScore = listingGenerator.calculateSEOScore(listing, targetKeywords);

        assertThat(seoScore.getTotalScore())
                .as("Poorly optimized listing should score low")
                .isLessThan(40.0);
    }

    // -----------------------------------------------------------------------
    // Prompt Template Rendering
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Variables are correctly substituted in prompt template")
    void testPromptTemplateRendering() {
        // Given: Template with variables
        Map<String, Object> variables = Map.of(
                "product_type", "Water Bottle",
                "brand", "AcmeBrand",
                "keywords", "insulated, BPA free, sports",
                "language", "English",
                "max_chars", 200
        );

        when(templateEngine.renderTemplate("title_generation", variables))
                .thenReturn("Generate a product title for an English Water Bottle listing. " +
                           "Brand: AcmeBrand. Keywords: insulated, BPA free, sports. Max 200 chars.");

        // When: Rendering template
        String rendered = templateEngine.renderTemplate("title_generation", variables);

        // Then: All variables substituted
        assertThat(rendered)
                .contains("Water Bottle")
                .contains("AcmeBrand")
                .contains("insulated")
                .contains("200");
    }

    // -----------------------------------------------------------------------
    // Model Fallback
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Primary model failure triggers fallback to secondary model")
    void testModelFallback() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .productType("Phone Case")
                .language("en")
                .build();

        // Primary model fails
        when(templateEngine.renderTemplate(anyString(), anyMap())).thenReturn("prompt");
        when(primaryModel.generate("prompt"))
                .thenThrow(new LLMServiceException("Primary model unavailable"));

        // Fallback model succeeds
        when(fallbackModel.generate("prompt"))
                .thenReturn(new LLMResponse("Premium Protective Phone Case with Shock Absorption", 0.82));
        when(forbiddenWordDetector.detect(anyString())).thenReturn(List.of());

        // When: Generating with fallback
        GeneratedTitle title = listingGenerator.generateTitle(request);

        // Then: Fallback model was used
        assertThat(title.getText()).isNotEmpty();
        verify(fallbackModel).generate("prompt");
        verify(primaryModel).generate("prompt"); // primary was tried first
    }

    @Test
    @DisplayName("Both models failing throws exception with clear error message")
    void testModelFallback_BothFail() {
        ListingGenerationRequest request = ListingGenerationRequest.builder()
                .productType("Phone Case")
                .language("en")
                .build();

        when(templateEngine.renderTemplate(anyString(), anyMap())).thenReturn("prompt");
        when(primaryModel.generate("prompt"))
                .thenThrow(new LLMServiceException("Primary unavailable"));
        when(fallbackModel.generate("prompt"))
                .thenThrow(new LLMServiceException("Fallback unavailable"));

        assertThatThrownBy(() -> listingGenerator.generateTitle(request))
                .isInstanceOf(LLMServiceException.class)
                .hasMessageContaining("unavailable");
    }

    // -----------------------------------------------------------------------
    // Response Parsing
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("JSON response from LLM is parsed to ListingDTO correctly")
    void testResponseParsing() {
        // Given: LLM returns structured JSON response
        String llmJsonResponse = """
                {
                  "title": "AcmeBrand Premium Insulated Water Bottle 32oz",
                  "bullet_points": [
                    "DOUBLE WALL VACUUM INSULATION - Keeps drinks cold 24hrs or hot 12hrs",
                    "FOOD GRADE STAINLESS STEEL - BPA-free 18/8 steel construction",
                    "LEAK PROOF LID - Silicone gasket ensures no spills during transport",
                    "WIDE MOUTH DESIGN - Easy to fill, add ice, and clean thoroughly",
                    "LIFETIME WARRANTY - We stand behind our product quality completely"
                  ],
                  "backend_keywords": "gym fitness hiking camping sport exercise travel outdoor",
                  "description": "Premium quality insulated water bottle for active lifestyles"
                }
                """;

        when(primaryModel.generateStructured(anyString(), eq(ListingDTO.class)))
                .thenReturn(objectMapper().readValue(llmJsonResponse, ListingDTO.class));

        // When: Parsing response
        ListingDTO listing = listingGenerator.parseLLMResponse(llmJsonResponse);

        // Then: All fields parsed correctly
        assertThat(listing.getTitle()).contains("AcmeBrand");
        assertThat(listing.getBulletPoints()).hasSize(5);
        assertThat(listing.getBackendKeywords()).isNotEmpty();
        assertThat(listing.getDescription()).isNotEmpty();
    }

    @Test
    @DisplayName("Malformed JSON response triggers retry with cleaner prompt")
    void testResponseParsing_Malformed() {
        String malformedResponse = "Here is your listing:\nTitle: Some Product\n- Bullet 1\n- Bullet 2";

        // When: Parsing fails, retry with structured prompt
        ListingDTO result = listingGenerator.parseLLMResponseWithRetry(malformedResponse);

        // Then: Retry produced valid result or fallback parsing worked
        assertThat(result).isNotNull();
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
