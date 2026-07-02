package cn.iocoder.yudao.module.amazon.ai.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the Amazon AI module.
 *
 * <p>Binds to the {@code yudao.amazon.ai.*} namespace in application.yml.
 * Covers model selection, API keys, budget limits, cache, and timeout settings.
 *
 * @author AmazonOps AI
 */
@Data
@ConfigurationProperties(prefix = "yudao.amazon.ai")
public class AiProperties {

    // -----------------------------------------------------------------------
    // OpenAI
    // -----------------------------------------------------------------------

    /** OpenAI API key (required for GPT-4o / GPT-4o-mini). */
    private String openaiApiKey;

    /** OpenAI base URL. Override for Azure OpenAI or local-compatible endpoints. */
    private String openaiBaseUrl = "https://api.openai.com";

    /** Primary model for high-quality tasks (listing generation, complex analysis). */
    private String primaryModel = "gpt-4o";

    /** Lightweight model for bulk / cost-effective tasks (sentiment, classification). */
    private String lightweightModel = "gpt-4o-mini";

    // -----------------------------------------------------------------------
    // Anthropic Claude (optional, used as fallback for complex analysis)
    // -----------------------------------------------------------------------

    /** Anthropic API key. */
    private String anthropicApiKey;

    /** Anthropic model name. */
    private String anthropicModel = "claude-sonnet-4-20250514";

    // -----------------------------------------------------------------------
    // Local / self-hosted model (optional)
    // -----------------------------------------------------------------------

    /** Base URL for a local OpenAI-compatible API (Ollama, vLLM, etc.). */
    private String localModelBaseUrl;

    /** Model name for the local endpoint. */
    private String localModelName = "qwen2.5:14b";

    // -----------------------------------------------------------------------
    // Budget & rate limiting
    // -----------------------------------------------------------------------

    /** Monthly token budget (input + output tokens combined). 0 = unlimited. */
    private long monthlyTokenBudget = 10_000_000L;

    /** Alert threshold as a percentage (0-100). Alert when usage exceeds this. */
    private int budgetAlertThreshold = 80;

    /** Maximum tokens per single request. */
    private int maxTokensPerRequest = 8192;

    /** Default temperature for LLM calls. Lower = more deterministic. */
    private double defaultTemperature = 0.3;

    // -----------------------------------------------------------------------
    // Timeout & retry
    // -----------------------------------------------------------------------

    /** HTTP request timeout for LLM API calls. */
    private Duration requestTimeout = Duration.ofSeconds(60);

    /** Maximum number of retries on transient failures. */
    private int maxRetries = 2;

    // -----------------------------------------------------------------------
    // Cache
    // -----------------------------------------------------------------------

    /** Whether to cache identical prompt responses. */
    private boolean cacheEnabled = true;

    /** Cache TTL (time-to-live). */
    private Duration cacheTtl = Duration.ofHours(6);

    // -----------------------------------------------------------------------
    // Per-task model overrides
    // -----------------------------------------------------------------------

    /**
     * Optional per-task model overrides.
     * Key = AiTaskType code, Value = model name.
     * When set, overrides the default model for that task type.
     */
    private Map<String, String> modelOverrides = new HashMap<>();

    // -----------------------------------------------------------------------
    // Prompt template settings
    // -----------------------------------------------------------------------

    /** Classpath prefix for prompt templates. */
    private String promptTemplatePath = "classpath:prompts/";

    // -----------------------------------------------------------------------
    // Feature flags
    // -----------------------------------------------------------------------

    /** Enable the AI chat assistant (OpsAgent). */
    private boolean chatAssistantEnabled = true;

    /** Enable token usage tracking and persistence. */
    private boolean tokenTrackingEnabled = true;
}
