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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI-powered listing quality diagnosis service.
 *
 * <p>Evaluates a listing against 12 quality dimensions and produces a score (0-100)
 * with specific, actionable improvement recommendations.
 *
 * <p>Diagnosis dimensions:
 * <ol>
 *   <li>Title length & structure</li>
 *   <li>Keyword density & coverage</li>
 *   <li>Bullet point quality</li>
 *   <li>Backend keyword optimization</li>
 *   <li>Forbidden words & compliance</li>
 *   <li>Image count & quality indicators</li>
 *   <li>Description quality</li>
 *   <li>Competitive positioning</li>
 *   <li>Search term optimization</li>
 *   <li>Readability & buyer experience</li>
 *   <li>Conversion elements</li>
 *   <li>Category-specific compliance</li>
 * </ol>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingDiagnosisService {

    private static final String TEMPLATE_NAME = "listing_diagnosis";

    // Rule-based scoring weights
    private static final int TITLE_WEIGHT = 12;
    private static final int KEYWORD_WEIGHT = 15;
    private static final int BULLET_WEIGHT = 12;
    private static final int BACKEND_WEIGHT = 8;
    private static final int COMPLIANCE_WEIGHT = 10;
    private static final int IMAGE_WEIGHT = 5;
    private static final int DESCRIPTION_WEIGHT = 8;
    private static final int POSITIONING_WEIGHT = 8;
    private static final int SEARCH_WEIGHT = 7;
    private static final int READABILITY_WEIGHT = 5;
    private static final int CONVERSION_WEIGHT = 5;
    private static final int CATEGORY_WEIGHT = 5;

    private final AiModelRouter modelRouter;
    private final PromptTemplateEngine templateEngine;
    private final AiTokenTracker tokenTracker;
    private final AiResponseParser responseParser;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Diagnose a listing and produce a comprehensive quality report.
     *
     * <p>Combines rule-based scoring (fast, deterministic) with LLM analysis
     * (nuanced, contextual) for a hybrid diagnosis.
     *
     * @param input the listing data to diagnose
     * @return diagnosis report with scores, issues, and prioritized actions
     */
    public DiagnosisReport diagnose(DiagnosisInput input) {
        log.info("Diagnosing listing: asin={}", input.getAsin());

        // Phase 1: Rule-based scoring (fast, deterministic)
        List<DimensionScore> ruleScores = runRuleBasedDiagnosis(input);

        // Phase 2: LLM-based analysis (nuanced, contextual)
        DiagnosisReport llmReport = runLlmDiagnosis(input);

        // Merge: use LLM analysis for qualitative insights, rules for quantitative scores
        DiagnosisReport merged = mergeReports(ruleScores, llmReport, input);

        log.info("Diagnosis complete: asin={}, score={}, grade={}",
                input.getAsin(), merged.getOverallScore(), merged.getGrade());

        return merged;
    }

    // -----------------------------------------------------------------------
    // Rule-based diagnosis
    // -----------------------------------------------------------------------

    private List<DimensionScore> runRuleBasedDiagnosis(DiagnosisInput input) {
        List<DimensionScore> scores = new ArrayList<>();

        // 1. Title length & structure
        scores.add(diagnoseTitle(input));

        // 2. Keyword density & coverage
        scores.add(diagnoseKeywords(input));

        // 3. Bullet point quality
        scores.add(diagnoseBulletPoints(input));

        // 4. Backend keyword optimization
        scores.add(diagnoseBackendKeywords(input));

        // 5. Forbidden words & compliance
        scores.add(diagnoseCompliance(input));

        // 6. Image count
        scores.add(diagnoseImages(input));

        // 7. Description quality
        scores.add(diagnoseDescription(input));

        // 8-12: Require LLM analysis (competitive positioning, search terms, etc.)
        // These will be filled by the LLM phase

        return scores;
    }

    private DimensionScore diagnoseTitle(DiagnosisInput input) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 100;

        if (input.getTitle() == null || input.getTitle().isBlank()) {
            return DimensionScore.builder()
                    .dimension("Title Length & Structure")
                    .score(0).status("FAIL")
                    .issues(List.of("Title is missing"))
                    .recommendations(List.of("Add a product title"))
                    .build();
        }

        int titleLen = input.getTitle().length();
        if (titleLen > 200) {
            score -= 30;
            issues.add("Title exceeds 200 characters (" + titleLen + " chars)");
            recommendations.add("Shorten title to 200 characters or less");
        } else if (titleLen < 50) {
            score -= 25;
            issues.add("Title is too short (" + titleLen + " chars), missing SEO opportunities");
            recommendations.add("Expand title to include more keywords (aim for 150-200 chars)");
        } else if (titleLen < 100) {
            score -= 10;
            issues.add("Title could be longer (" + titleLen + " chars)");
            recommendations.add("Consider adding more keywords (150-200 chars is optimal)");
        }

        // Check brand at start
        if (input.getBrand() != null && !input.getTitle().startsWith(input.getBrand())) {
            score -= 15;
            issues.add("Title does not start with brand name");
            recommendations.add("Move brand name to the beginning of the title");
        }

        // Check for ALL CAPS (bad practice)
        long upperCount = input.getTitle().chars().filter(Character::isUpperCase).count();
        if (upperCount > titleLen * 0.5 && titleLen > 10) {
            score -= 10;
            issues.add("Title uses excessive capitalization");
            recommendations.add("Use Title Case instead of ALL CAPS");
        }

        String status = score >= 80 ? "PASS" : score >= 50 ? "WARN" : "FAIL";
        return DimensionScore.builder()
                .dimension("Title Length & Structure")
                .score(score).status(status)
                .issues(issues).recommendations(recommendations)
                .build();
    }

    private DimensionScore diagnoseKeywords(DiagnosisInput input) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (input.getTargetKeywords() == null || input.getTargetKeywords().isEmpty()) {
            return DimensionScore.builder()
                    .dimension("Keyword Density & Coverage")
                    .score(50).status("WARN")
                    .issues(List.of("No target keywords provided for analysis"))
                    .recommendations(List.of("Define target keywords to measure coverage"))
                    .build();
        }

        String allListingText = buildAllListingText(input).toLowerCase();
        int found = 0;
        List<String> missing = new ArrayList<>();

        for (String kw : input.getTargetKeywords()) {
            if (allListingText.contains(kw.toLowerCase())) {
                found++;
            } else {
                missing.add(kw);
            }
        }

        double coverage = (found * 100.0) / input.getTargetKeywords().size();
        int score = (int) Math.min(coverage, 100);

        if (coverage < 50) {
            issues.add("Only " + found + "/" + input.getTargetKeywords().size() + " target keywords found in listing");
            recommendations.add("Add missing keywords: " + String.join(", ", missing));
        } else if (coverage < 80) {
            issues.add(found + "/" + input.getTargetKeywords().size() + " keywords covered, room for improvement");
            recommendations.add("Add these missing keywords: " + String.join(", ", missing));
        }

        // Check keyword in title
        String titleLower = input.getTitle() != null ? input.getTitle().toLowerCase() : "";
        long titleKws = input.getTargetKeywords().stream()
                .filter(kw -> titleLower.contains(kw.toLowerCase()))
                .count();
        if (titleKws == 0) {
            score -= 15;
            issues.add("No target keywords found in title");
            recommendations.add("Include at least the primary keyword in the title");
        }

        String status = score >= 80 ? "PASS" : score >= 50 ? "WARN" : "FAIL";
        return DimensionScore.builder()
                .dimension("Keyword Density & Coverage")
                .score(Math.max(score, 0)).status(status)
                .issues(issues).recommendations(recommendations)
                .build();
    }

    private DimensionScore diagnoseBulletPoints(DiagnosisInput input) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 100;

        if (input.getBulletPoints() == null || input.getBulletPoints().isEmpty()) {
            return DimensionScore.builder()
                    .dimension("Bullet Point Quality")
                    .score(0).status("FAIL")
                    .issues(List.of("No bullet points found"))
                    .recommendations(List.of("Add 5 bullet points, each 200-250 characters"))
                    .build();
        }

        int count = input.getBulletPoints().size();
        if (count < 5) {
            score -= (5 - count) * 15;
            issues.add("Only " + count + " bullet points (should be 5)");
            recommendations.add("Add " + (5 - count) + " more bullet points");
        } else if (count > 5) {
            score -= 5;
            issues.add(count + " bullet points (Amazon displays 5, extras may not show)");
        }

        for (int i = 0; i < input.getBulletPoints().size(); i++) {
            String bullet = input.getBulletPoints().get(i);
            int len = bullet.length();
            if (len < 150) {
                score -= 8;
                issues.add("Bullet " + (i + 1) + " is too short (" + len + " chars)");
                recommendations.add("Expand bullet " + (i + 1) + " with more detail (target 200-250 chars)");
            } else if (len > 250) {
                score -= 3;
                issues.add("Bullet " + (i + 1) + " exceeds optimal length (" + len + " chars)");
            }

            // Check for CAPITALIZED header
            if (!bullet.substring(0, Math.min(bullet.length(), 20)).matches(".*[A-Z]{2,}.*")) {
                score -= 5;
                issues.add("Bullet " + (i + 1) + " doesn't start with a CAPITALIZED benefit phrase");
                recommendations.add("Add a CAPITALIZED benefit header to bullet " + (i + 1));
            }
        }

        score = Math.max(score, 0);
        String status = score >= 80 ? "PASS" : score >= 50 ? "WARN" : "FAIL";
        return DimensionScore.builder()
                .dimension("Bullet Point Quality")
                .score(score).status(status)
                .issues(issues).recommendations(recommendations)
                .build();
    }

    private DimensionScore diagnoseBackendKeywords(DiagnosisInput input) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 100;

        if (input.getBackendKeywords() == null || input.getBackendKeywords().isBlank()) {
            return DimensionScore.builder()
                    .dimension("Backend Keyword Optimization")
                    .score(0).status("FAIL")
                    .issues(List.of("No backend keywords configured"))
                    .recommendations(List.of("Add backend search terms (up to 250 bytes)"))
                    .build();
        }

        byte[] bytes = input.getBackendKeywords().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 250) {
            score -= 20;
            issues.add("Backend keywords exceed 250 bytes (" + bytes.length + " bytes)");
            recommendations.add("Reduce backend keywords to within 250 bytes");
        } else if (bytes.length < 100) {
            score -= 15;
            issues.add("Backend keywords underutilized (" + bytes.length + "/250 bytes)");
            recommendations.add("Add more synonyms, misspellings, and related terms");
        }

        // Check for commas (should be space-separated)
        if (input.getBackendKeywords().contains(",")) {
            score -= 10;
            issues.add("Backend keywords contain commas (should be space-separated)");
            recommendations.add("Remove commas, use spaces only");
        }

        // Check for duplicates
        String[] words = input.getBackendKeywords().toLowerCase().split("\\s+");
        long unique = Arrays.stream(words).distinct().count();
        if (unique < words.length) {
            score -= 10;
            issues.add("Backend keywords contain " + (words.length - unique) + " duplicate words");
            recommendations.add("Remove duplicate words from backend keywords");
        }

        score = Math.max(score, 0);
        String status = score >= 80 ? "PASS" : score >= 50 ? "WARN" : "FAIL";
        return DimensionScore.builder()
                .dimension("Backend Keyword Optimization")
                .score(score).status(status)
                .issues(issues).recommendations(recommendations)
                .build();
    }

    private DimensionScore diagnoseCompliance(DiagnosisInput input) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 100;

        String allText = buildAllListingText(input).toLowerCase();
        List<String> forbidden = List.of(
                "best seller", "best-selling", "#1 rated", "number one",
                "guaranteed", "warranty", "free shipping", "cheapest",
                "lowest price", "risk free", "money back", "satisfaction guaranteed"
        );

        for (String word : forbidden) {
            if (allText.contains(word)) {
                score -= 15;
                issues.add("Forbidden phrase found: '" + word + "'");
                recommendations.add("Remove '" + word + "' — violates Amazon TOS");
            }
        }

        score = Math.max(score, 0);
        String status = score >= 80 ? "PASS" : score >= 50 ? "WARN" : "FAIL";
        return DimensionScore.builder()
                .dimension("Forbidden Words & Compliance")
                .score(score).status(status)
                .issues(issues).recommendations(recommendations)
                .build();
    }

    private DimensionScore diagnoseImages(DiagnosisInput input) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 100;

        if (input.getImageCount() < 5) {
            score -= 40;
            issues.add("Only " + input.getImageCount() + " images (minimum 7 recommended)");
            recommendations.add("Add at least 7 images including infographics and lifestyle shots");
        } else if (input.getImageCount() < 7) {
            score -= 15;
            issues.add(input.getImageCount() + " images (7-9 recommended for optimal conversion)");
            recommendations.add("Add more images — include lifestyle shots and infographics");
        }

        score = Math.max(score, 0);
        String status = score >= 80 ? "PASS" : score >= 50 ? "WARN" : "FAIL";
        return DimensionScore.builder()
                .dimension("Image Count & Quality")
                .score(score).status(status)
                .issues(issues).recommendations(recommendations)
                .build();
    }

    private DimensionScore diagnoseDescription(DiagnosisInput input) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 100;

        if (input.getDescription() == null || input.getDescription().isBlank()) {
            return DimensionScore.builder()
                    .dimension("Description Quality")
                    .score(0).status("FAIL")
                    .issues(List.of("No product description"))
                    .recommendations(List.of("Add a product description (1000-2000 characters)"))
                    .build();
        }

        int len = input.getDescription().length();
        if (len < 500) {
            score -= 30;
            issues.add("Description too short (" + len + " chars)");
            recommendations.add("Expand description to 1000-2000 characters with more detail");
        } else if (len < 1000) {
            score -= 10;
            issues.add("Description could be longer (" + len + " chars)");
            recommendations.add("Consider expanding to 1000-2000 characters");
        } else if (len > 2000) {
            score -= 5;
            issues.add("Description is quite long (" + len + " chars) — may be truncated");
        }

        score = Math.max(score, 0);
        String status = score >= 80 ? "PASS" : score >= 50 ? "WARN" : "FAIL";
        return DimensionScore.builder()
                .dimension("Description Quality")
                .score(score).status(status)
                .issues(issues).recommendations(recommendations)
                .build();
    }

    // -----------------------------------------------------------------------
    // LLM-based diagnosis
    // -----------------------------------------------------------------------

    private DiagnosisReport runLlmDiagnosis(DiagnosisInput input) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("asin", input.getAsin() != null ? input.getAsin() : "N/A");
        vars.put("title", input.getTitle() != null ? input.getTitle() : "N/A");
        vars.put("brand", input.getBrand() != null ? input.getBrand() : "N/A");
        vars.put("bulletPoints", input.getBulletPoints() != null
                ? String.join("\n", input.getBulletPoints()) : "N/A");
        vars.put("description", input.getDescription() != null ? input.getDescription() : "N/A");
        vars.put("backendKeywords", input.getBackendKeywords() != null ? input.getBackendKeywords() : "N/A");
        vars.put("category", input.getCategory() != null ? input.getCategory() : "General");
        vars.put("imageCount", String.valueOf(input.getImageCount()));
        vars.put("price", input.getPrice() != null ? input.getPrice() : "N/A");
        vars.put("rating", input.getRating() != null ? input.getRating() : "N/A");
        vars.put("reviewCount", String.valueOf(input.getReviewCount()));
        vars.put("targetKeywords", input.getTargetKeywords() != null
                ? String.join(", ", input.getTargetKeywords()) : "N/A");

        String prompt = templateEngine.render(TEMPLATE_NAME, vars);

        ChatClient client = modelRouter.getChatClient(AiTaskType.LISTING_DIAGNOSIS);

        try {
            ChatResponse response = client.prompt()
                    .system("You are an Amazon listing quality auditor. Return your diagnosis as valid JSON.")
                    .user(prompt)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();

            // Track tokens
            trackUsage(response);

            return responseParser.parse(content, DiagnosisReport.class);
        } catch (Exception e) {
            log.error("LLM diagnosis failed, returning rule-based results only: {}", e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Merge reports
    // -----------------------------------------------------------------------

    private DiagnosisReport mergeReports(List<DimensionScore> ruleScores,
                                         DiagnosisReport llmReport,
                                         DiagnosisInput input) {
        // Calculate overall score from rule-based dimensions
        int totalScore = ruleScores.stream().mapToInt(DimensionScore::getScore).sum();
        int maxPossible = ruleScores.size() * 100;
        int overallScore = (int) ((totalScore * 100.0) / maxPossible);

        // If LLM report is available, blend the scores
        if (llmReport != null && llmReport.getOverallScore() > 0) {
            overallScore = (overallScore + llmReport.getOverallScore()) / 2;
        }

        // Determine grade
        String grade;
        if (overallScore >= 90) grade = "A";
        else if (overallScore >= 80) grade = "B";
        else if (overallScore >= 65) grade = "C";
        else if (overallScore >= 50) grade = "D";
        else grade = "F";

        // Collect critical issues
        List<String> criticalIssues = new ArrayList<>();
        List<String> quickWins = new ArrayList<>();
        for (DimensionScore dim : ruleScores) {
            if ("FAIL".equals(dim.getStatus())) {
                criticalIssues.addAll(dim.getIssues());
            }
            if ("WARN".equals(dim.getStatus())) {
                quickWins.addAll(dim.getRecommendations());
            }
        }
        if (llmReport != null) {
            if (llmReport.getCriticalIssues() != null) criticalIssues.addAll(llmReport.getCriticalIssues());
            if (llmReport.getQuickWins() != null) quickWins.addAll(llmReport.getQuickWins());
        }

        return DiagnosisReport.builder()
                .overallScore(overallScore)
                .grade(grade)
                .dimensionScores(ruleScores)
                .criticalIssues(criticalIssues)
                .quickWins(quickWins)
                .keywordGaps(llmReport != null ? llmReport.getKeywordGaps() : List.of())
                .competitiveGaps(llmReport != null ? llmReport.getCompetitiveGaps() : List.of())
                .priorityActions(llmReport != null ? llmReport.getPriorityActions() : List.of())
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String buildAllListingText(DiagnosisInput input) {
        StringBuilder sb = new StringBuilder();
        if (input.getTitle() != null) sb.append(input.getTitle()).append(" ");
        if (input.getBulletPoints() != null) {
            sb.append(String.join(" ", input.getBulletPoints())).append(" ");
        }
        if (input.getDescription() != null) sb.append(input.getDescription()).append(" ");
        if (input.getBackendKeywords() != null) sb.append(input.getBackendKeywords());
        return sb.toString();
    }

    private void trackUsage(ChatResponse response) {
        try {
            var metadata = response.getMetadata();
            if (metadata != null && metadata.getUsage() != null) {
                tokenTracker.track(
                        AiTaskType.LISTING_DIAGNOSIS,
                        modelRouter.resolveModel(AiTaskType.LISTING_DIAGNOSIS),
                        (int) metadata.getUsage().getPromptTokens(),
                        (int) metadata.getUsage().getCompletionTokens()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to track token usage: {}", e.getMessage());
        }
    }
}
