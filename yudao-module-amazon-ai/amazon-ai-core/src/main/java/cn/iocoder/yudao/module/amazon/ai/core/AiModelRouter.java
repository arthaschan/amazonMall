package cn.iocoder.yudao.module.amazon.ai.core;

import cn.iocoder.yudao.module.amazon.ai.core.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes AI requests to the appropriate model based on task type.
 *
 * <p>Routing strategy (mirrors omniscient's factory.py pattern):
 * <ul>
 *   <li>LISTING_GENERATION / COMPLEX_ANALYSIS / CHAT_ASSISTANT -> GPT-4o (high quality)</li>
 *   <li>SENTIMENT_ANALYSIS / SIMPLE_CLASSIFICATION / LISTING_DIAGNOSIS -> GPT-4o-mini (cost-effective)</li>
 *   <li>INVENTORY_FORECAST -> Traditional ML (Prophet), NOT LLM</li>
 * </ul>
 *
 * <p>Supports per-task model overrides via {@link AiProperties#getModelOverrides()} and
 * automatic fallback when the primary model is unavailable.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class AiModelRouter {

    private final AiProperties aiProperties;

    /**
     * Cache of ChatClient instances keyed by model name to avoid re-creation.
     */
    private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();

    public AiModelRouter(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Get the resolved model name for a given task type, considering overrides.
     *
     * @param taskType the AI task type
     * @return the model name to use
     */
    public String resolveModel(AiTaskType taskType) {
        // Check per-task overrides first
        String override = aiProperties.getModelOverrides().get(taskType.getCode());
        if (override != null && !override.isBlank()) {
            return override;
        }
        return taskType.getDefaultModel();
    }

    /**
     * Get a {@link ChatClient} configured for the given task type.
     * The client is cached and reused across calls.
     *
     * <p>This is the primary entry point for all LLM-based services.
     *
     * @param taskType the task type
     * @return a ChatClient ready for use
     * @throws UnsupportedOperationException if the task type uses non-LLM models (e.g. Prophet)
     */
    public ChatClient getChatClient(AiTaskType taskType) {
        if (taskType == AiTaskType.INVENTORY_FORECAST) {
            throw new UnsupportedOperationException(
                    "INVENTORY_FORECAST uses traditional ML (Prophet/LSTM), not an LLM. " +
                    "Use AiInventoryForecastService instead.");
        }

        String model = resolveModel(taskType);
        return chatClientCache.computeIfAbsent(model, this::buildChatClient);
    }

    /**
     * Get a fallback ChatClient when the primary model fails.
     * Falls back to the alternative model tier (4o -> 4o-mini, or Claude if configured).
     *
     * @param taskType the original task type
     * @return a fallback ChatClient
     */
    public ChatClient getFallbackChatClient(AiTaskType taskType) {
        String fallbackModel = resolveFallbackModel(taskType);
        log.warn("Primary model [{}] unavailable for task [{}], falling back to [{}]",
                resolveModel(taskType), taskType.getCode(), fallbackModel);
        return chatClientCache.computeIfAbsent(fallbackModel, this::buildChatClient);
    }

    /**
     * Build {@link OpenAiChatOptions} with the correct model name, temperature,
     * and max tokens for the given task type.
     *
     * @param taskType the task type
     * @return chat options
     */
    public OpenAiChatOptions buildChatOptions(AiTaskType taskType) {
        return OpenAiChatOptions.builder()
                .model(resolveModel(taskType))
                .temperature(aiProperties.getDefaultTemperature())
                .maxTokens(aiProperties.getMaxTokensPerRequest())
                .build();
    }

    /**
     * Build chat options with JSON response format forced.
     * Useful for structured output from LLMs.
     */
    public OpenAiChatOptions buildJsonChatOptions(AiTaskType taskType) {
        return OpenAiChatOptions.builder()
                .model(resolveModel(taskType))
                .temperature(aiProperties.getDefaultTemperature())
                .maxTokens(aiProperties.getMaxTokensPerRequest())
                .responseFormat("json_object")
                .build();
    }

    /**
     * Check whether a task type uses an LLM or traditional ML.
     */
    public boolean isLlmTask(AiTaskType taskType) {
        return taskType != AiTaskType.INVENTORY_FORECAST;
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private ChatClient buildChatClient(String model) {
        log.info("Building ChatClient for model: {}", model);

        String apiKey = aiProperties.getOpenaiApiKey();
        String baseUrl = aiProperties.getOpenaiBaseUrl();

        // If this is the Anthropic model, check if Claude is configured
        if (model.startsWith("claude") && aiProperties.getAnthropicApiKey() != null) {
            // For Claude, we still use the OpenAI-compatible wrapper in Spring AI
            // This requires a proxy or the Anthropic OpenAI-compatible endpoint
            log.info("Claude model requested [{}]; ensure an OpenAI-compatible proxy is configured", model);
        }

        // If this is a local model name, route to the local endpoint
        if (model.equals(aiProperties.getLocalModelName()) && aiProperties.getLocalModelBaseUrl() != null) {
            baseUrl = aiProperties.getLocalModelBaseUrl();
            apiKey = "not-needed";
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey != null ? apiKey : "")
                .baseUrl(baseUrl != null ? baseUrl : "https://api.openai.com")
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(aiProperties.getDefaultTemperature())
                        .maxTokens(aiProperties.getMaxTokensPerRequest())
                        .build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private String resolveFallbackModel(AiTaskType taskType) {
        String primary = resolveModel(taskType);

        // If primary is the full model, fall back to mini
        if (primary.equals(aiProperties.getPrimaryModel())) {
            return aiProperties.getLightweightModel();
        }

        // If Anthropic is configured, use it as fallback for complex tasks
        if (aiProperties.getAnthropicApiKey() != null
                && (taskType == AiTaskType.COMPLEX_ANALYSIS || taskType == AiTaskType.LISTING_GENERATION)) {
            return aiProperties.getAnthropicModel();
        }

        // Default: swap tiers
        if (primary.equals(aiProperties.getLightweightModel())) {
            return aiProperties.getPrimaryModel();
        }

        return aiProperties.getLightweightModel();
    }
}
