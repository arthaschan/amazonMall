package cn.iocoder.yudao.module.amazon.ai.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTOs for the AI Inventory Forecast module.
 *
 * @author AmazonOps AI
 */
public class InventoryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesHistory {
        private String asin;
        /** Daily sales data: date -> units sold */
        private Map<LocalDate, Integer> dailySales;
        private Integer currentStock;
        private Integer inboundStock;
        private Double avgDailySales;
        private Double standardDeviation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastConfig {
        /** Number of days to forecast */
        private Integer forecastDays;
        /** Confidence level for intervals (e.g. 80, 95) */
        private Integer confidenceLevel;
        /** Lead time in days (supplier to FBA warehouse) */
        private Integer leadTimeDays;
        /** Minimum safety stock days */
        private Integer safetyStockDays;
        /** Whether to apply seasonal adjustment */
        private Boolean seasonalAdjustment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryForecast {
        private String asin;
        private LocalDate forecastStartDate;
        private LocalDate forecastEndDate;
        /** Daily forecast: date -> predicted units */
        private List<DailyForecast> dailyForecasts;
        private ReorderRecommendation reorderRecommendation;
        private SeasonalAnalysis seasonalAnalysis;
        private ForecastAccuracy accuracy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private LocalDate date;
        private Double predictedUnits;
        private Double lowerBound;
        private Double upperBound;
        private Boolean isSeasonalPeak;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderRecommendation {
        private LocalDate reorderDate;
        private Integer reorderQuantity;
        private Integer safetyStockUnits;
        private Integer daysOfSupplyRemaining;
        private String urgency; // CRITICAL, HIGH, MEDIUM, LOW
        private Double estimatedStockoutDate;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalAnalysis {
        private Boolean hasSeasonalPattern;
        private List<SeasonalPeriod> peakPeriods;
        private List<SeasonalPeriod> lowPeriods;
        private Double seasonalAmplitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalPeriod {
        private String period;
        private Double multiplier;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastAccuracy {
        private Double mape; // Mean Absolute Percentage Error
        private Double rmse; // Root Mean Square Error
        private String confidenceLevel; // HIGH, MEDIUM, LOW
    }
}
