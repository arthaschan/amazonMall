package cn.iocoder.yudao.module.amazon.ai.core;

import cn.iocoder.yudao.module.amazon.ai.core.dal.dataobject.AiTokenUsageDO;
import cn.iocoder.yudao.module.amazon.ai.core.dal.mysql.AiTokenUsageMapper;
import cn.iocoder.yudao.module.amazon.ai.core.enums.AiTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Tracks AI token usage per request and enforces monthly budget limits.
 *
 * <p>Features:
 * <ul>
 *   <li>Per-request input/output token counting</li>
 *   <li>Monthly budget tracking with configurable alert threshold</li>
 *   <li>Asynchronous persistence to avoid blocking LLM calls</li>
 *   <li>In-memory counters for fast budget checks</li>
 *   <li>Cost estimation based on model pricing</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTokenTracker {

    private final AiProperties aiProperties;
    private final AiTokenUsageMapper tokenUsageMapper;

    /**
     * In-memory monthly token counter for fast budget checks.
     * Key: "tenantId:yearMonth", Value: total tokens used this month.
     */
    private final Map<String, AtomicLong> monthlyTokenCounters =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Optional callback invoked when budget alert threshold is exceeded.
     * Inject via {@link #setBudgetAlertCallback(Consumer)}.
     */
    private Consumer<BudgetAlert> budgetAlertCallback;

    // -----------------------------------------------------------------------
    // Model pricing (USD per 1M tokens)
    // -----------------------------------------------------------------------

    private static final Map<String, double[]> MODEL_PRICING = Map.of(
            // [inputCostPerMillion, outputCostPerMillion]
            "gpt-4o",            new double[]{2.50, 10.00},
            "gpt-4o-mini",       new double[]{0.15,  0.60},
            "gpt-4-turbo",       new double[]{10.00, 30.00},
            "gpt-3.5-turbo",     new double[]{0.50,  1.50},
            "claude-sonnet-4-20250514", new double[]{3.00, 15.00}
    );

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Record token usage from an AI request. This is the main entry point.
     *
     * <p>Persists asynchronously and updates the in-memory budget counter.
     *
     * @param taskType    the AI task type
     * @param model       the model used
     * @param inputTokens number of prompt tokens
     * @param outputTokens number of completion tokens
     * @param businessId  optional business entity ID (ASIN, listing ID, etc.)
     * @param userId      optional user ID who initiated the request
     * @param tenantId    tenant ID for multi-tenancy
     */
    public void track(AiTaskType taskType, String model,
                      int inputTokens, int outputTokens,
                      String businessId, Long userId, Long tenantId) {
        if (!aiProperties.isTokenTrackingEnabled()) {
            return;
        }

        int totalTokens = inputTokens + outputTokens;
        double cost = estimateCost(model, inputTokens, outputTokens);
        String yearMonth = currentYearMonth();

        // Update in-memory counter
        String counterKey = tenantId + ":" + yearMonth;
        long currentTotal = monthlyTokenCounters
                .computeIfAbsent(counterKey, k -> new AtomicLong(0))
                .addAndGet(totalTokens);

        // Check budget alert
        checkBudgetAlert(tenantId, currentTotal, yearMonth);

        // Persist asynchronously
        persistUsageAsync(taskType, model, inputTokens, outputTokens, totalTokens,
                cost, businessId, userId, tenantId, yearMonth);
    }

    /**
     * Simplified tracking without business context.
     */
    public void track(AiTaskType taskType, String model, int inputTokens, int outputTokens) {
        track(taskType, model, inputTokens, outputTokens, null, null, 0L);
    }

    /**
     * Check whether the monthly budget has been exceeded for a tenant.
     *
     * @param tenantId the tenant ID
     * @return true if the budget allows more requests
     */
    public boolean isWithinBudget(Long tenantId) {
        if (aiProperties.getMonthlyTokenBudget() <= 0) {
            return true; // Unlimited
        }
        String counterKey = tenantId + ":" + currentYearMonth();
        long used = monthlyTokenCounters
                .computeIfAbsent(counterKey, k -> new AtomicLong(0))
                .get();
        return used < aiProperties.getMonthlyTokenBudget();
    }

    /**
     * Get the current month's token usage for a tenant.
     *
     * @param tenantId the tenant ID
     * @return total tokens used this month
     */
    public long getMonthlyUsage(Long tenantId) {
        String counterKey = tenantId + ":" + currentYearMonth();
        return monthlyTokenCounters
                .computeIfAbsent(counterKey, k -> new AtomicLong(0))
                .get();
    }

    /**
     * Get the current month's estimated cost for a tenant.
     * Falls back to database query if in-memory counter is cold.
     */
    public double getMonthlyCost(Long tenantId) {
        return tokenUsageMapper.sumCostByTenantAndMonth(tenantId, currentYearMonth());
    }

    /**
     * Estimate cost for a given model and token counts.
     *
     * @param model        model name
     * @param inputTokens  prompt tokens
     * @param outputTokens completion tokens
     * @return estimated cost in USD
     */
    public double estimateCost(String model, int inputTokens, int outputTokens) {
        double[] pricing = MODEL_PRICING.getOrDefault(model, new double[]{5.0, 15.0});
        return (inputTokens * pricing[0] / 1_000_000.0)
             + (outputTokens * pricing[1] / 1_000_000.0);
    }

    /**
     * Register a callback for budget alert notifications.
     */
    public void setBudgetAlertCallback(Consumer<BudgetAlert> callback) {
        this.budgetAlertCallback = callback;
    }

    /**
     * Initialize the in-memory counter from the database on application startup.
     * Call this from an ApplicationRunner or similar.
     */
    public void warmUpCounter(Long tenantId) {
        String yearMonth = currentYearMonth();
        long dbTotal = tokenUsageMapper.sumTokensByTenantAndMonth(tenantId, yearMonth);
        String counterKey = tenantId + ":" + yearMonth;
        monthlyTokenCounters.computeIfAbsent(counterKey, k -> new AtomicLong(dbTotal));
        log.info("Warmed up token counter for tenant [{}], month [{}]: {} tokens",
                tenantId, yearMonth, dbTotal);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    @Async
    protected void persistUsageAsync(AiTaskType taskType, String model,
                                     int inputTokens, int outputTokens, int totalTokens,
                                     double cost, String businessId, Long userId,
                                     Long tenantId, String yearMonth) {
        try {
            AiTokenUsageDO usage = new AiTokenUsageDO();
            usage.setTaskType(taskType.getCode());
            usage.setModel(model);
            usage.setInputTokens(inputTokens);
            usage.setOutputTokens(outputTokens);
            usage.setTotalTokens(totalTokens);
            usage.setEstimatedCostUsd(cost);
            usage.setBusinessId(businessId);
            usage.setUserId(userId);
            usage.setTenantId(tenantId);
            usage.setYearMonth(yearMonth);

            tokenUsageMapper.insert(usage);
            log.debug("Persisted token usage: task={}, model={}, tokens={}/{}, cost=${}",
                    taskType.getCode(), model, inputTokens, outputTokens,
                    String.format("%.6f", cost));
        } catch (Exception e) {
            log.error("Failed to persist AI token usage: {}", e.getMessage(), e);
        }
    }

    private void checkBudgetAlert(Long tenantId, long currentTotal, String yearMonth) {
        long budget = aiProperties.getMonthlyTokenBudget();
        if (budget <= 0) return;

        double usagePct = (currentTotal * 100.0) / budget;
        int threshold = aiProperties.getBudgetAlertThreshold();

        if (usagePct >= threshold && budgetAlertCallback != null) {
            log.warn("AI budget alert: tenant [{}] has used {}/{} tokens ({}%) for month [{}]",
                    tenantId, currentTotal, budget, String.format("%.1f", usagePct), yearMonth);
            budgetAlertCallback.accept(new BudgetAlert(tenantId, currentTotal, budget, usagePct, yearMonth));
        }
    }

    private String currentYearMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    // -----------------------------------------------------------------------
    // DTO
    // -----------------------------------------------------------------------

    /**
     * Budget alert event fired when usage exceeds the configured threshold.
     */
    public record BudgetAlert(
            Long tenantId,
            long tokensUsed,
            long budgetLimit,
            double usagePercent,
            String yearMonth
    ) {}
}
