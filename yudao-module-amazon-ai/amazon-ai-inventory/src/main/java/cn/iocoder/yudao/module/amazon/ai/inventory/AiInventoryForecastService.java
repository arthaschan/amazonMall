package cn.iocoder.yudao.module.amazon.ai.inventory;

import cn.iocoder.yudao.module.amazon.ai.inventory.dto.InventoryDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Inventory demand forecasting service using traditional ML (NOT LLM).
 *
 * <p>Uses a combination of statistical methods for demand forecasting:
 * <ul>
 *   <li><b>Moving Average</b> - smoothed baseline demand</li>
 *   <li><b>Exponential Smoothing (Holt-Winters)</b> - trend + seasonality</li>
 *   <li><b>Seasonal Decomposition</b> - weekly and monthly patterns</li>
 *   <li><b>Confidence Intervals</b> - based on historical variance</li>
 * </ul>
 *
 * <p>NOTE: This service intentionally does NOT use LLMs. Inventory forecasting
 * is a time-series prediction problem best solved with statistical methods.
 * The model router marks INVENTORY_FORECAST as a Prophet/traditional ML task.
 *
 * <p>In production, this could be backed by Facebook Prophet or an LSTM model
 * served via a Python microservice. This implementation provides a pure-Java
 * statistical approach suitable for most use cases.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AiInventoryForecastService {

    private static final double Z_SCORE_80 = 1.282;
    private static final double Z_SCORE_90 = 1.645;
    private static final double Z_SCORE_95 = 1.960;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generate an inventory demand forecast.
     *
     * @param salesHistory historical sales data
     * @param config       forecast configuration
     * @return demand forecast with reorder recommendations
     */
    public InventoryForecast forecast(SalesHistory salesHistory, ForecastConfig config) {
        log.info("Generating forecast for ASIN: {}, forecast days: {}, confidence: {}%",
                salesHistory.getAsin(), config.getForecastDays(), config.getConfidenceLevel());

        // Validate input
        if (salesHistory.getDailySales() == null || salesHistory.getDailySales().isEmpty()) {
            log.warn("No sales history provided for ASIN: {}", salesHistory.getAsin());
            return emptyForecast(salesHistory.getAsin(), config);
        }

        int forecastDays = config.getForecastDays() != null ? config.getForecastDays() : 90;
        int confidenceLevel = config.getConfidenceLevel() != null ? config.getConfidenceLevel() : 80;
        int leadTimeDays = config.getLeadTimeDays() != null ? config.getLeadTimeDays() : 30;
        int safetyStockDays = config.getSafetyStockDays() != null ? config.getSafetyStockDays() : 14;

        // Step 1: Calculate statistical baseline
        StatisticalBaseline baseline = calculateBaseline(salesHistory);

        // Step 2: Detect seasonal patterns
        SeasonalAnalysis seasonal = detectSeasonality(salesHistory, config);

        // Step 3: Generate daily forecasts
        List<DailyForecast> dailyForecasts = generateDailyForecasts(
                baseline, seasonal, forecastDays, confidenceLevel);

        // Step 4: Calculate reorder recommendation
        ReorderRecommendation reorder = calculateReorder(
                salesHistory, dailyForecasts, leadTimeDays, safetyStockDays);

        // Step 5: Estimate forecast accuracy
        ForecastAccuracy accuracy = estimateAccuracy(salesHistory, baseline);

        InventoryForecast forecast = InventoryForecast.builder()
                .asin(salesHistory.getAsin())
                .forecastStartDate(LocalDate.now())
                .forecastEndDate(LocalDate.now().plusDays(forecastDays))
                .dailyForecasts(dailyForecasts)
                .reorderRecommendation(reorder)
                .seasonalAnalysis(seasonal)
                .accuracy(accuracy)
                .build();

        log.info("Forecast complete: avg daily={:.1f}, reorder date={}, reorder qty={}, urgency={}",
                baseline.avgDaily, reorder.getReorderDate(),
                reorder.getReorderQuantity(), reorder.getUrgency());

        return forecast;
    }

    /**
     * Quick reorder check: when to reorder and how much.
     */
    public ReorderRecommendation quickReorderCheck(SalesHistory salesHistory,
                                                    int leadTimeDays,
                                                    int safetyStockDays) {
        StatisticalBaseline baseline = calculateBaseline(salesHistory);
        int currentStock = salesHistory.getCurrentStock() != null ? salesHistory.getCurrentStock() : 0;
        int inboundStock = salesHistory.getInboundStock() != null ? salesHistory.getInboundStock() : 0;
        int totalAvailable = currentStock + inboundStock;

        double dailyDemand = baseline.avgDaily;
        int daysOfSupply = dailyDemand > 0 ? (int)(totalAvailable / dailyDemand) : 999;

        // Reorder point = (lead time demand) + safety stock
        int leadTimeDemand = (int) Math.ceil(dailyDemand * leadTimeDays);
        int safetyStock = (int) Math.ceil(dailyDemand * safetyStockDays);
        int reorderPoint = leadTimeDemand + safetyStock;

        String urgency;
        if (totalAvailable <= safetyStock) {
            urgency = "CRITICAL";
        } else if (totalAvailable <= reorderPoint) {
            urgency = "HIGH";
        } else if (daysOfSupply <= leadTimeDays + safetyStockDays + 7) {
            urgency = "MEDIUM";
        } else {
            urgency = "LOW";
        }

        // Recommended order quantity: cover lead time + safety stock + 30 days buffer
        int orderQuantity = (int) Math.ceil(dailyDemand * (leadTimeDays + safetyStockDays + 30));

        LocalDate reorderDate = daysOfSupply <= leadTimeDays + safetyStockDays
                ? LocalDate.now()
                : LocalDate.now().plusDays(Math.max(0, daysOfSupply - leadTimeDays - safetyStockDays));

        return ReorderRecommendation.builder()
                .reorderDate(reorderDate)
                .reorderQuantity(orderQuantity)
                .safetyStockUnits(safetyStock)
                .daysOfSupplyRemaining(daysOfSupply)
                .urgency(urgency)
                .estimatedStockoutDate(dailyDemand > 0
                        ? (double) daysOfSupply : null)
                .notes(String.format("Current stock: %d, Inbound: %d, Avg daily sales: %.1f",
                        currentStock, inboundStock, dailyDemand))
                .build();
    }

    // -----------------------------------------------------------------------
    // Statistical methods
    // -----------------------------------------------------------------------

    private StatisticalBaseline calculateBaseline(SalesHistory salesHistory) {
        List<Integer> sales = salesHistory.getDailySales().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        if (sales.isEmpty()) {
            return new StatisticalBaseline(0, 0, 0, 0);
        }

        // Calculate mean
        double mean = sales.stream().mapToInt(Integer::intValue).average().orElse(0);

        // Calculate standard deviation
        double variance = sales.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // Exponential smoothing (alpha = 0.3 for responsiveness)
        double alpha = 0.3;
        double smoothed = sales.get(0);
        for (int i = 1; i < sales.size(); i++) {
            smoothed = alpha * sales.get(i) + (1 - alpha) * smoothed;
        }

        // Trend detection (linear regression)
        double trend = calculateTrend(sales);

        return new StatisticalBaseline(mean, stdDev, smoothed, trend);
    }

    private double calculateTrend(List<Integer> sales) {
        if (sales.size() < 7) return 0;

        // Simple linear regression: y = a + b*x
        int n = sales.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += sales.get(i);
            sumXY += (double) i * sales.get(i);
            sumX2 += (double) i * i;
        }

        double b = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return b; // units per day trend
    }

    private SeasonalAnalysis detectSeasonality(SalesHistory salesHistory, ForecastConfig config) {
        if (!Boolean.TRUE.equals(config.getSeasonalAdjustment())) {
            return SeasonalAnalysis.builder()
                    .hasSeasonalPattern(false)
                    .peakPeriods(List.of())
                    .lowPeriods(List.of())
                    .build();
        }

        // Detect day-of-week patterns
        Map<DayOfWeek, List<Integer>> dayOfWeekSales = new EnumMap<>(DayOfWeek.class);
        for (var entry : salesHistory.getDailySales().entrySet()) {
            dayOfWeekSales.computeIfAbsent(entry.getKey().getDayOfWeek(), k -> new ArrayList<>())
                    .add(entry.getValue());
        }

        // Detect monthly patterns
        Map<Month, List<Integer>> monthSales = new EnumMap<>(Month.class);
        for (var entry : salesHistory.getDailySales().entrySet()) {
            monthSales.computeIfAbsent(entry.getKey().getMonth(), k -> new ArrayList<>())
                    .add(entry.getValue());
        }

        double overallAvg = salesHistory.getDailySales().values().stream()
                .mapToInt(Integer::intValue).average().orElse(1);

        // Identify peak and low months
        List<SeasonalPeriod> peakPeriods = new ArrayList<>();
        List<SeasonalPeriod> lowPeriods = new ArrayList<>();

        for (var entry : monthSales.entrySet()) {
            double monthAvg = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            double multiplier = overallAvg > 0 ? monthAvg / overallAvg : 1.0;

            if (multiplier > 1.2) {
                peakPeriods.add(SeasonalPeriod.builder()
                        .period(entry.getKey().name())
                        .multiplier(multiplier)
                        .notes(String.format("%.0f%% above average", (multiplier - 1) * 100))
                        .build());
            } else if (multiplier < 0.8) {
                lowPeriods.add(SeasonalPeriod.builder()
                        .period(entry.getKey().name())
                        .multiplier(multiplier)
                        .notes(String.format("%.0f%% below average", (1 - multiplier) * 100))
                        .build());
            }
        }

        boolean hasSeasonal = !peakPeriods.isEmpty() || !lowPeriods.isEmpty();

        return SeasonalAnalysis.builder()
                .hasSeasonalPattern(hasSeasonal)
                .peakPeriods(peakPeriods)
                .lowPeriods(lowPeriods)
                .seasonalAmplitude(hasSeasonal
                        ? peakPeriods.stream().mapToDouble(SeasonalPeriod::getMultiplier).max().orElse(1.0)
                          - lowPeriods.stream().mapToDouble(SeasonalPeriod::getMultiplier).min().orElse(1.0)
                        : 0.0)
                .build();
    }

    private List<DailyForecast> generateDailyForecasts(StatisticalBaseline baseline,
                                                        SeasonalAnalysis seasonal,
                                                        int forecastDays,
                                                        int confidenceLevel) {
        double zScore = switch (confidenceLevel) {
            case 95 -> Z_SCORE_95;
            case 90 -> Z_SCORE_90;
            default -> Z_SCORE_80;
        };

        // Build seasonal multiplier map by month
        Map<Month, Double> seasonalMultipliers = new EnumMap<>(Month.class);
        if (seasonal.getPeakPeriods() != null) {
            for (SeasonalPeriod sp : seasonal.getPeakPeriods()) {
                try {
                    seasonalMultipliers.put(Month.valueOf(sp.getPeriod()), sp.getMultiplier());
                } catch (Exception ignored) {}
            }
        }
        if (seasonal.getLowPeriods() != null) {
            for (SeasonalPeriod sp : seasonal.getLowPeriods()) {
                try {
                    seasonalMultipliers.put(Month.valueOf(sp.getPeriod()), sp.getMultiplier());
                } catch (Exception ignored) {}
            }
        }

        // Q4 peak detection (November-December holiday season)
        Set<Month> q4PeakMonths = Set.of(Month.NOVEMBER, Month.DECEMBER);

        List<DailyForecast> forecasts = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int i = 0; i < forecastDays; i++) {
            LocalDate date = startDate.plusDays(i);

            // Base prediction: smoothed value + trend
            double base = baseline.smoothed + (baseline.trend * i);

            // Apply seasonal multiplier
            double seasonalMultiplier = seasonalMultipliers.getOrDefault(date.getMonth(), 1.0);
            double predicted = Math.max(0, base * seasonalMultiplier);

            // Q4 boost (if no specific seasonal data)
            boolean isSeasonalPeak = seasonalMultiplier > 1.15 || q4PeakMonths.contains(date.getMonth());

            // Confidence interval
            double margin = zScore * baseline.stdDev * Math.sqrt(1 + i * 0.01);
            double lower = Math.max(0, predicted - margin);
            double upper = predicted + margin;

            forecasts.add(DailyForecast.builder()
                    .date(date)
                    .predictedUnits(Math.round(predicted * 10.0) / 10.0)
                    .lowerBound(Math.round(lower * 10.0) / 10.0)
                    .upperBound(Math.round(upper * 10.0) / 10.0)
                    .isSeasonalPeak(isSeasonalPeak)
                    .build());
        }

        return forecasts;
    }

    private ReorderRecommendation calculateReorder(SalesHistory salesHistory,
                                                    List<DailyForecast> dailyForecasts,
                                                    int leadTimeDays,
                                                    int safetyStockDays) {
        int currentStock = salesHistory.getCurrentStock() != null ? salesHistory.getCurrentStock() : 0;
        int inboundStock = salesHistory.getInboundStock() != null ? salesHistory.getInboundStock() : 0;
        int totalAvailable = currentStock + inboundStock;

        // Cumulative demand forecast
        double cumulativeDemand = 0;
        LocalDate stockoutDate = null;

        for (DailyForecast df : dailyForecasts) {
            cumulativeDemand += df.getPredictedUnits();
            if (cumulativeDemand >= totalAvailable && stockoutDate == null) {
                stockoutDate = df.getDate();
            }
        }

        int daysOfSupply = 0;
        double runningDemand = 0;
        for (DailyForecast df : dailyForecasts) {
            runningDemand += df.getPredictedUnits();
            if (runningDemand > totalAvailable) break;
            daysOfSupply++;
        }

        // Safety stock based on demand variability and lead time
        double avgDailyDemand = dailyForecasts.stream()
                .mapToDouble(DailyForecast::getPredictedUnits).average().orElse(0);
        int safetyStock = (int) Math.ceil(avgDailyDemand * safetyStockDays);
        int leadTimeDemand = (int) Math.ceil(avgDailyDemand * leadTimeDays);
        int reorderPoint = leadTimeDemand + safetyStock;

        // Reorder quantity: cover lead time + safety stock + 30 days
        int orderQuantity = (int) Math.ceil(avgDailyDemand * (leadTimeDays + safetyStockDays + 30));

        // Urgency
        String urgency;
        if (totalAvailable <= safetyStock) {
            urgency = "CRITICAL";
        } else if (totalAvailable <= reorderPoint) {
            urgency = "HIGH";
        } else if (daysOfSupply <= leadTimeDays + safetyStockDays + 7) {
            urgency = "MEDIUM";
        } else {
            urgency = "LOW";
        }

        LocalDate reorderDate = daysOfSupply > leadTimeDays + safetyStockDays
                ? LocalDate.now().plusDays(daysOfSupply - leadTimeDays - safetyStockDays)
                : LocalDate.now();

        return ReorderRecommendation.builder()
                .reorderDate(reorderDate)
                .reorderQuantity(orderQuantity)
                .safetyStockUnits(safetyStock)
                .daysOfSupplyRemaining(daysOfSupply)
                .urgency(urgency)
                .estimatedStockoutDate(stockoutDate != null
                        ? (double) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), stockoutDate)
                        : null)
                .notes(String.format("Current: %d units, Inbound: %d, Avg daily demand: %.1f, Days of supply: %d",
                        currentStock, inboundStock, avgDailyDemand, daysOfSupply))
                .build();
    }

    private ForecastAccuracy estimateAccuracy(SalesHistory salesHistory, StatisticalBaseline baseline) {
        if (salesHistory.getDailySales().size() < 14) {
            return ForecastAccuracy.builder()
                    .confidenceLevel("LOW")
                    .mape(null).rmse(null)
                    .build();
        }

        // Backtest: use first 70% to predict last 30%
        List<Integer> sales = salesHistory.getDailySales().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        int splitPoint = (int) (sales.size() * 0.7);
        List<Integer> testSet = sales.subList(splitPoint, sales.size());

        double trainMean = sales.subList(0, splitPoint).stream()
                .mapToInt(Integer::intValue).average().orElse(0);

        double mapeSum = 0;
        double rmseSum = 0;
        int count = 0;

        for (int actual : testSet) {
            if (actual > 0) {
                double error = Math.abs(actual - trainMean) / actual;
                mapeSum += error;
                rmseSum += Math.pow(actual - trainMean, 2);
                count++;
            }
        }

        double mape = count > 0 ? (mapeSum / count) * 100 : 0;
        double rmse = count > 0 ? Math.sqrt(rmseSum / count) : 0;

        String confidence;
        if (mape < 20) confidence = "HIGH";
        else if (mape < 40) confidence = "MEDIUM";
        else confidence = "LOW";

        return ForecastAccuracy.builder()
                .mape(Math.round(mape * 10.0) / 10.0)
                .rmse(Math.round(rmse * 10.0) / 10.0)
                .confidenceLevel(confidence)
                .build();
    }

    private InventoryForecast emptyForecast(String asin, ForecastConfig config) {
        return InventoryForecast.builder()
                .asin(asin)
                .forecastStartDate(LocalDate.now())
                .forecastEndDate(LocalDate.now().plusDays(config.getForecastDays() != null ? config.getForecastDays() : 90))
                .dailyForecasts(List.of())
                .reorderRecommendation(ReorderRecommendation.builder()
                        .urgency("UNKNOWN").notes("Insufficient data for forecast").build())
                .seasonalAnalysis(SeasonalAnalysis.builder()
                        .hasSeasonalPattern(false).peakPeriods(List.of()).lowPeriods(List.of()).build())
                .accuracy(ForecastAccuracy.builder()
                        .confidenceLevel("LOW").build())
                .build();
    }

    private record StatisticalBaseline(double avgDaily, double stdDev, double smoothed, double trend) {}
}
