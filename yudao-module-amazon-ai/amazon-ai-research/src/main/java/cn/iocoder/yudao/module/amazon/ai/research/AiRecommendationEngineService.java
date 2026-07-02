package cn.iocoder.yudao.module.amazon.ai.research;

import cn.iocoder.yudao.module.amazon.ai.core.AiModelRouter;
import cn.iocoder.yudao.module.amazon.ai.core.AiResponseParser;
import cn.iocoder.yudao.module.amazon.ai.core.AiTokenTracker;
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
 * AI-powered niche recommendation engine.
 *
 * <p>Scores and ranks product niches based on market data, then generates
 * actionable recommendations for each niche including entry strategy,
 * estimated costs, and risk assessment.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationEngineService {

    private final AiModelRouter modelRouter;
    private final AiTokenTracker tokenTracker;
    private final AiResponseParser responseParser;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generate ranked recommendations for a list of niche opportunities.
     *
     * <p>Combines quantitative scoring (rule-based) with qualitative AI analysis
     * to produce ranked recommendations with specific action items.
     *
     * @param niches list of niche market data to evaluate
     * @return ranked recommendations, best opportunities first
     */
    public List<Recommendation> generateRecommendations(List<Niche> niches) {
        if (niches == null || niches.isEmpty()) {
            log.warn("No niches provided for recommendation");
            return List.of();
        }

        log.info("Generating recommendations for {} niches", niches.size());

        // Phase 1: Rule-based scoring
        List<ScoredNiche> scoredNiches = niches.stream()
                .map(this::scoreNiche)
                .sorted(Comparator.comparingDouble(ScoredNiche::score).reversed())
                .collect(Collectors.toList());

        // Phase 2: LLM-enhanced analysis for top niches
        List<Recommendation> recommendations = new ArrayList<>();
        int topN = Math.min(scoredNiches.size(), 10);

        for (int i = 0; i < topN; i++) {
            ScoredNiche scored = scoredNiches.get(i);
            Recommendation llmEnhanced = enhanceWithLlm(scored, i + 1, scoredNiches.size());
            if (llmEnhanced != null) {
                recommendations.add(llmEnhanced);
            } else {
                // Fallback to rule-based recommendation
                recommendations.add(buildRuleBasedRecommendation(scored, i + 1));
            }
        }

        // Add remaining niches with rule-based recommendations only
        for (int i = topN; i < scoredNiches.size(); i++) {
            recommendations.add(buildRuleBasedRecommendation(scoredNiches.get(i), i + 1));
        }

        return recommendations;
    }

    // -----------------------------------------------------------------------
    // Rule-based scoring
    // -----------------------------------------------------------------------

    private ScoredNiche scoreNiche(Niche niche) {
        double score = 0;

        // Market demand (avg monthly sales)
        if (niche.getAvgMonthlySales() != null) {
            if (niche.getAvgMonthlySales() > 300) score += 20;
            else if (niche.getAvgMonthlySales() > 100) score += 15;
            else if (niche.getAvgMonthlySales() > 50) score += 10;
            else score += 5;
        }

        // Price point (sweet spot: $15-$50)
        if (niche.getAvgPrice() != null) {
            if (niche.getAvgPrice() >= 20 && niche.getAvgPrice() <= 50) score += 20;
            else if (niche.getAvgPrice() >= 15 && niche.getAvgPrice() <= 75) score += 15;
            else if (niche.getAvgPrice() < 15) score += 5; // too low margin
            else score += 10;
        }

        // Competition level
        if (niche.getNumberOfCompetitors() != null) {
            if (niche.getNumberOfCompetitors() < 50) score += 20;
            else if (niche.getNumberOfCompetitors() < 200) score += 15;
            else if (niche.getNumberOfCompetitors() < 500) score += 10;
            else score += 5;
        }

        // Review barrier (average review count of competitors)
        if (niche.getAvgReviewCount() != null) {
            if (niche.getAvgReviewCount() < 100) score += 15;
            else if (niche.getAvgReviewCount() < 500) score += 10;
            else if (niche.getAvgReviewCount() < 1000) score += 5;
            else score += 0; // very high barrier
        }

        // Rating opportunity (room for improvement)
        if (niche.getAvgRating() != null) {
            if (niche.getAvgRating() < 4.0) score += 15; // lots of room to improve
            else if (niche.getAvgRating() < 4.3) score += 10;
            else if (niche.getAvgRating() < 4.5) score += 5;
            else score += 0; // hard to differentiate
        }

        // Profit potential
        if (niche.getEstimatedProfit() != null) {
            if (niche.getEstimatedProfit() > 5000) score += 10;
            else if (niche.getEstimatedProfit() > 2000) score += 7;
            else if (niche.getEstimatedProfit() > 500) score += 4;
        }

        return new ScoredNiche(niche, Math.min(score, 100));
    }

    // -----------------------------------------------------------------------
    // LLM-enhanced analysis
    // -----------------------------------------------------------------------

    private Recommendation enhanceWithLlm(ScoredNiche scored, int rank, int totalNiches) {
        Niche niche = scored.niche();

        String prompt = """
                Analyze this Amazon niche opportunity and provide a detailed recommendation.

                NICHE DATA:
                - Keyword: %s
                - Average Price: $%.2f
                - Average Monthly Sales: %d units
                - Average Rating: %.1f stars
                - Average Review Count: %d
                - Number of Competitors: %d
                - Estimated Monthly Revenue: $%.2f
                - Estimated Monthly Profit: $%.2f
                - Competition Level: %s
                - Quantitative Score: %.0f/100 (rank %d of %d)

                Provide a recommendation covering:
                1. Overall recommendation (GO / CONDITIONAL GO / NO GO)
                2. Key strengths of this niche (2-3 points)
                3. Key weaknesses and risks (2-3 points)
                4. Specific action items for entry (3-5 steps)
                5. Risk level (LOW/MEDIUM/HIGH)
                6. Estimated startup cost range
                7. Estimated time to launch
                8. Key success factors

                Return JSON:
                {
                    "niche_keyword": "%s",
                    "overall_score": %.0f,
                    "recommendation": "<GO/CONDITIONAL GO/NO GO with explanation>",
                    "strengths": ["<strength1>", "<strength2>"],
                    "weaknesses": ["<weakness1>", "<weakness2>"],
                    "action_items": ["<action1>", "<action2>", "<action3>"],
                    "risk_level": "<LOW/MEDIUM/HIGH>",
                    "estimated_monthly_profit": %.2f,
                    "estimated_startup_cost": <integer>,
                    "time_to_launch": "<weeks>"
                }
                """.formatted(
                niche.getKeyword(),
                niche.getAvgPrice() != null ? niche.getAvgPrice() : 0.0,
                niche.getAvgMonthlySales() != null ? niche.getAvgMonthlySales() : 0,
                niche.getAvgRating() != null ? niche.getAvgRating() : 0.0,
                niche.getAvgReviewCount() != null ? niche.getAvgReviewCount() : 0,
                niche.getNumberOfCompetitors() != null ? niche.getNumberOfCompetitors() : 0,
                niche.getEstimatedRevenue() != null ? niche.getEstimatedRevenue() : 0.0,
                niche.getEstimatedProfit() != null ? niche.getEstimatedProfit() : 0.0,
                niche.getCompetitionLevel() != null ? niche.getCompetitionLevel() : "unknown",
                scored.score(), rank, totalNiches,
                niche.getKeyword(), scored.score(),
                niche.getEstimatedProfit() != null ? niche.getEstimatedProfit() : 0.0
        );

        try {
            String response = callLlm(AiTaskType.COMPLEX_ANALYSIS, prompt,
                    "You are an Amazon product selection strategist. Be conservative and evidence-based. Return valid JSON.");

            return responseParser.parse(response, Recommendation.class);
        } catch (Exception e) {
            log.warn("LLM enhancement failed for niche '{}': {}", niche.getKeyword(), e.getMessage());
            return null;
        }
    }

    private Recommendation buildRuleBasedRecommendation(ScoredNiche scored, int rank) {
        Niche niche = scored.niche();
        double score = scored.score();

        String recommendation;
        String riskLevel;
        if (score >= 70) {
            recommendation = "GO - Strong opportunity with good demand, manageable competition, and profit potential.";
            riskLevel = "LOW";
        } else if (score >= 50) {
            recommendation = "CONDITIONAL GO - Viable but requires careful execution. Key risks need mitigation.";
            riskLevel = "MEDIUM";
        } else {
            recommendation = "NO GO - Significant barriers or insufficient profit potential. Consider alternatives.";
            riskLevel = "HIGH";
        }

        List<String> strengths = new ArrayList<>();
        if (niche.getAvgMonthlySales() != null && niche.getAvgMonthlySales() > 200) {
            strengths.add("Strong demand: " + niche.getAvgMonthlySales() + " avg monthly sales");
        }
        if (niche.getAvgPrice() != null && niche.getAvgPrice() >= 20 && niche.getAvgPrice() <= 50) {
            strengths.add("Good price point ($" + String.format("%.0f", niche.getAvgPrice()) + ") for private label");
        }
        if (niche.getNumberOfCompetitors() != null && niche.getNumberOfCompetitors() < 100) {
            strengths.add("Manageable competition: " + niche.getNumberOfCompetitors() + " competitors");
        }
        if (strengths.isEmpty()) strengths.add("Niche has potential based on available data");

        List<String> weaknesses = new ArrayList<>();
        if (niche.getAvgReviewCount() != null && niche.getAvgReviewCount() > 500) {
            weaknesses.add("High review barrier: avg " + niche.getAvgReviewCount() + " reviews per competitor");
        }
        if (niche.getAvgRating() != null && niche.getAvgRating() >= 4.5) {
            weaknesses.add("Competitors well-rated (" + String.format("%.1f", niche.getAvgRating()) + "★) — hard to differentiate");
        }
        if (niche.getAvgPrice() != null && niche.getAvgPrice() < 15) {
            weaknesses.add("Low price point limits margin potential");
        }
        if (weaknesses.isEmpty()) weaknesses.add("Monitor competition and market trends");

        List<String> actionItems = List.of(
                "Source product samples from 3+ suppliers on Alibaba",
                "Order competitor products for quality comparison",
                "Calculate detailed unit economics including all FBA fees",
                "Develop differentiated product concept based on review gaps",
                "Create launch plan with PPC budget and review strategy"
        );

        return Recommendation.builder()
                .nicheKeyword(niche.getKeyword())
                .overallScore(score)
                .recommendation(recommendation)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .actionItems(actionItems)
                .riskLevel(riskLevel)
                .estimatedMonthlyProfit(niche.getEstimatedProfit() != null ? niche.getEstimatedProfit() : 0.0)
                .estimatedStartupCost(niche.getAvgPrice() != null
                        ? (int)(niche.getAvgPrice() * 500 * 0.3) : 5000)
                .timeToLaunch("8-12 weeks")
                .build();
    }

    // -----------------------------------------------------------------------
    // LLM call
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

    private record ScoredNiche(Niche niche, double score) {}
}
