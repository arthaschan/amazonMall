package cn.iocoder.yudao.module.amazon.ai.core.enums;

/**
 * AI task types used for model routing.
 * Each task type maps to a specific model tier optimized for cost and quality.
 *
 * @author AmazonOps AI
 */
public enum AiTaskType {

    /** Listing title/bullet/backend-keyword generation (high quality output needed) */
    LISTING_GENERATION("listing_generation", "gpt-4o"),

    /** Product blueprint, competitive analysis (high quality, complex reasoning) */
    COMPLEX_ANALYSIS("complex_analysis", "gpt-4o"),

    /** Review sentiment tagging (cost-effective for bulk processing) */
    SENTIMENT_ANALYSIS("sentiment_analysis", "gpt-4o-mini"),

    /** Simple text classification tasks */
    SIMPLE_CLASSIFICATION("simple_classification", "gpt-4o-mini"),

    /** Chat assistant with Function Calling support */
    CHAT_ASSISTANT("chat_assistant", "gpt-4o"),

    /** Inventory forecasting - uses traditional ML (Prophet), NOT LLM */
    INVENTORY_FORECAST("inventory_forecast", "prophet"),

    /** Ad optimization suggestions */
    AD_OPTIMIZATION("ad_optimization", "gpt-4o"),

    /** Weekly report generation */
    WEEKLY_REPORT("weekly_report", "gpt-4o"),

    /** Listing quality diagnosis */
    LISTING_DIAGNOSIS("listing_diagnosis", "gpt-4o-mini");

    private final String code;
    private final String defaultModel;

    AiTaskType(String code, String defaultModel) {
        this.code = code;
        this.defaultModel = defaultModel;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}
