package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.module.amazon.ai.inventory.AiInventoryForecastService;
import cn.iocoder.yudao.module.amazon.ai.inventory.dto.InventoryDtos.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Function tool: Get inventory demand forecast and reorder recommendations.
 *
 * <p>Delegates to {@link AiInventoryForecastService} for the actual forecast.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class ForecastInventoryFunction implements Function<ForecastInventoryFunction.Request, ForecastInventoryFunction.Response> {

    private final AiInventoryForecastService forecastService;

    public ForecastInventoryFunction(AiInventoryForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String asin;
        /** Number of days to forecast */
        private Integer forecastDays;
        /** Confidence level (0-100, typically 80 or 95) */
        private Integer confidenceLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String asin;
        private Integer forecastDays;
        private Double avgDailyDemand;
        private Double predictedTotalDemand;
        private String reorderDate;
        private Integer reorderQuantity;
        private Integer daysOfSupplyRemaining;
        private String urgency;
        private String seasonalInsight;
        private String confidenceLevel;
        private String summary;
    }

    @Override
    public Response apply(Request request) {
        log.info("forecastInventory called: asin={}, forecastDays={}, confidence={}",
                request.getAsin(), request.getForecastDays(), request.getConfidenceLevel());

        int forecastDays = request.getForecastDays() != null ? request.getForecastDays() : 90;
        int confidence = request.getConfidenceLevel() != null ? request.getConfidenceLevel() : 80;

        // TODO: Fetch actual sales history from the inventory/order service
        // For now, build a sample history to demonstrate the integration

        Map<LocalDate, Integer> sampleHistory = generateSampleHistory();

        SalesHistory salesHistory = SalesHistory.builder()
                .asin(request.getAsin() != null ? request.getAsin() : "B08N5WRWNW")
                .dailySales(sampleHistory)
                .currentStock(150)
                .inboundStock(0)
                .build();

        ForecastConfig config = ForecastConfig.builder()
                .forecastDays(forecastDays)
                .confidenceLevel(confidence)
                .leadTimeDays(30)
                .safetyStockDays(14)
                .seasonalAdjustment(true)
                .build();

        try {
            InventoryForecast forecast = forecastService.forecast(salesHistory, config);
            ReorderRecommendation reorder = forecast.getReorderRecommendation();

            double avgDaily = forecast.getDailyForecasts().stream()
                    .mapToDouble(DailyForecast::getPredictedUnits)
                    .average().orElse(0);
            double totalDemand = forecast.getDailyForecasts().stream()
                    .mapToDouble(DailyForecast::getPredictedUnits)
                    .sum();

            String seasonalInsight = forecast.getSeasonalAnalysis() != null
                    && Boolean.TRUE.equals(forecast.getSeasonalAnalysis().getHasSeasonalPattern())
                    ? "Seasonal pattern detected. Peak periods: "
                        + forecast.getSeasonalAnalysis().getPeakPeriods().stream()
                            .map(SeasonalPeriod::getPeriod)
                            .reduce((a, b) -> a + ", " + b).orElse("none")
                    : "No significant seasonal pattern detected.";

            return Response.builder()
                    .asin(request.getAsin())
                    .forecastDays(forecastDays)
                    .avgDailyDemand(Math.round(avgDaily * 10.0) / 10.0)
                    .predictedTotalDemand(Math.round(totalDemand * 10.0) / 10.0)
                    .reorderDate(reorder.getReorderDate() != null
                            ? reorder.getReorderDate().toString() : "Unknown")
                    .reorderQuantity(reorder.getReorderQuantity())
                    .daysOfSupplyRemaining(reorder.getDaysOfSupplyRemaining())
                    .urgency(reorder.getUrgency())
                    .seasonalInsight(seasonalInsight)
                    .confidenceLevel(forecast.getAccuracy() != null
                            ? forecast.getAccuracy().getConfidenceLevel() : "LOW")
                    .summary(String.format(
                            "Forecast: %.1f avg daily units over %d days. Total predicted demand: %.0f units. "
                            + "Current stock covers %d days. Reorder %d units by %s (%s urgency).",
                            avgDaily, forecastDays, totalDemand,
                            reorder.getDaysOfSupplyRemaining(),
                            reorder.getReorderQuantity(),
                            reorder.getReorderDate(),
                            reorder.getUrgency()))
                    .build();
        } catch (Exception e) {
            log.error("Forecast failed: {}", e.getMessage(), e);
            return Response.builder()
                    .asin(request.getAsin())
                    .urgency("ERROR")
                    .summary("Forecast generation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Generate 60 days of sample sales history for demonstration.
     */
    private Map<LocalDate, Integer> generateSampleHistory() {
        Map<LocalDate, Integer> history = new TreeMap<>();
        LocalDate today = LocalDate.now();
        java.util.Random random = new java.util.Random(42);

        for (int i = 60; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            // Base demand with some noise
            int dailySales = Math.max(0, (int)(3.5 + random.nextGaussian() * 1.5));
            history.put(date, dailySales);
        }
        return history;
    }
}
