package cn.iocoder.yudao.module.amazon.ai.agent;

import cn.iocoder.yudao.module.amazon.ai.agent.functions.*;
import cn.iocoder.yudao.module.amazon.ai.core.AiModelRouter;
import cn.iocoder.yudao.module.amazon.ai.core.AiProperties;
import cn.iocoder.yudao.module.amazon.ai.core.AiTokenTracker;
import cn.iocoder.yudao.module.amazon.ai.core.PromptTemplateEngine;
import cn.iocoder.yudao.module.amazon.ai.core.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Operations Agent — a Function Calling powered conversational assistant
 * for Amazon sellers.
 *
 * <p>Provides a natural language interface for sellers to:
 * <ul>
 *   <li>Query sales, inventory, ad, and review data</li>
 *   <li>Generate listings and analyze reviews</li>
 *   <li>Get keyword data and inventory forecasts</li>
 *   <li>Receive proactive alerts and recommendations</li>
 * </ul>
 *
 * <p>Uses Spring AI's ChatClient with FunctionCallback registrations for
 * each tool function. The LLM decides which tools to call based on the
 * conversation context.
 *
 * <p>Supports both blocking and streaming responses.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class OpsAgent {

    private static final String TEMPLATE_NAME = "chat_assistant";

    private final AiModelRouter modelRouter;
    private final PromptTemplateEngine templateEngine;
    private final AiTokenTracker tokenTracker;
    private final AiProperties aiProperties;

    /** Registered function tools */
    private final List<FunctionCallback> functionCallbacks;

    /**
     * Conversation history per session.
     * Key: sessionId, Value: list of messages (role + content).
     */
    private final Map<String, List<ChatMessage>> conversationHistory =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final int MAX_HISTORY_MESSAGES = 20;

    public OpsAgent(AiModelRouter modelRouter,
                    PromptTemplateEngine templateEngine,
                    AiTokenTracker tokenTracker,
                    AiProperties aiProperties,
                    GetSalesSummaryFunction salesSummaryFunction,
                    GetTopProductsFunction topProductsFunction,
                    GetInventoryStatusFunction inventoryStatusFunction,
                    GetAdPerformanceFunction adPerformanceFunction,
                    AnalyzeReviewsFunction analyzeReviewsFunction,
                    GenerateListingFunction generateListingFunction,
                    GetKeywordDataFunction keywordDataFunction,
                    ForecastInventoryFunction forecastInventoryFunction) {
        this.modelRouter = modelRouter;
        this.templateEngine = templateEngine;
        this.tokenTracker = tokenTracker;
        this.aiProperties = aiProperties;

        // Register all function tools as FunctionCallbacks
        this.functionCallbacks = List.of(
                FunctionCallback.builder()
                        .function("getSalesSummary", salesSummaryFunction)
                        .description("Retrieve sales performance data. Parameters: dateRange (e.g. 'last 7 days', 'this month'), asin (optional)")
                        .inputType(GetSalesSummaryFunction.Request.class)
                        .build(),

                FunctionCallback.builder()
                        .function("getTopProducts", topProductsFunction)
                        .description("Get best/worst performing products. Parameters: metric ('revenue'|'units'|'profit'), limit, order ('asc'|'desc')")
                        .inputType(GetTopProductsFunction.Request.class)
                        .build(),

                FunctionCallback.builder()
                        .function("getInventoryStatus", inventoryStatusFunction)
                        .description("Check inventory levels, days of supply, and restock alerts. Parameters: asin (optional), includeAlerts (boolean)")
                        .inputType(GetInventoryStatusFunction.Request.class)
                        .build(),

                FunctionCallback.builder()
                        .function("getAdPerformance", adPerformanceFunction)
                        .description("Retrieve advertising campaign performance. Parameters: dateRange, campaignName (optional), includeSearchTerms (boolean)")
                        .inputType(GetAdPerformanceFunction.Request.class)
                        .build(),

                FunctionCallback.builder()
                        .function("analyzeReviews", analyzeReviewsFunction)
                        .description("Run AI analysis on product reviews. Parameters: asin, analysisType ('sentiment'|'pain_points'|'selling_points'|'personas')")
                        .inputType(AnalyzeReviewsFunction.Request.class)
                        .build(),

                FunctionCallback.builder()
                        .function("generateListing", generateListingFunction)
                        .description("Generate an optimized product listing. Parameters: asin or productType, targetKeywords (list), language")
                        .inputType(GenerateListingFunction.Request.class)
                        .build(),

                FunctionCallback.builder()
                        .function("getKeywordData", keywordDataFunction)
                        .description("Retrieve keyword ranking and search volume. Parameters: keyword, asin (optional for rank tracking)")
                        .inputType(GetKeywordDataFunction.Request.class)
                        .build(),

                FunctionCallback.builder()
                        .function("forecastInventory", forecastInventoryFunction)
                        .description("Get inventory demand forecast and reorder recommendations. Parameters: asin, forecastDays, confidenceLevel (0-100)")
                        .inputType(ForecastInventoryFunction.Request.class)
                        .build()
        );

        log.info("OpsAgent initialized with {} function tools", functionCallbacks.size());
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Process a user message and return a complete response.
     *
     * @param sessionId the conversation session ID
     * @param userMessage the user's message
     * @param tenantId the tenant ID for data isolation
     * @return the agent's response text
     */
    public String chat(String sessionId, String userMessage, Long tenantId) {
        log.info("Chat request: session={}, message={}", sessionId,
                userMessage.length() > 100 ? userMessage.substring(0, 100) + "..." : userMessage);

        // Add user message to history
        addToHistory(sessionId, "user", userMessage);

        // Build the system prompt
        String systemPrompt = loadSystemPrompt();

        // Get ChatClient with function calling support
        ChatClient chatClient = modelRouter.getChatClient(AiTaskType.CHAT_ASSISTANT);

        // Build the prompt with conversation history and function tools
        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(systemPrompt);

        // Add conversation history
        List<ChatMessage> history = conversationHistory.getOrDefault(sessionId, List.of());
        for (ChatMessage msg : history) {
            if ("user".equals(msg.role)) {
                requestSpec = requestSpec.user(msg.content);
            }
        }

        // Register function tools
        for (FunctionCallback callback : functionCallbacks) {
            requestSpec = requestSpec.functions(callback);
        }

        // Execute
        try {
            ChatResponse response = requestSpec.call().chatResponse();
            String assistantMessage = response.getResult().getOutput().getText();

            // Track token usage
            trackUsage(response);

            // Add assistant response to history
            addToHistory(sessionId, "assistant", assistantMessage);

            return assistantMessage;
        } catch (Exception e) {
            log.error("Chat processing failed: {}", e.getMessage(), e);
            String errorResponse = "I apologize, but I encountered an error processing your request. "
                    + "Please try again or rephrase your question.";
            addToHistory(sessionId, "assistant", errorResponse);
            return errorResponse;
        }
    }

    /**
     * Process a user message and stream the response.
     *
     * @param sessionId   the conversation session ID
     * @param userMessage the user's message
     * @param tenantId    the tenant ID
     * @return a Flux of response text chunks
     */
    public Flux<String> chatStream(String sessionId, String userMessage, Long tenantId) {
        log.info("Streaming chat request: session={}", sessionId);

        addToHistory(sessionId, "user", userMessage);

        String systemPrompt = loadSystemPrompt();
        ChatClient chatClient = modelRouter.getChatClient(AiTaskType.CHAT_ASSISTANT);

        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(systemPrompt);

        List<ChatMessage> history = conversationHistory.getOrDefault(sessionId, List.of());
        for (ChatMessage msg : history) {
            if ("user".equals(msg.role)) {
                requestSpec = requestSpec.user(msg.content);
            }
        }

        for (FunctionCallback callback : functionCallbacks) {
            requestSpec = requestSpec.functions(callback);
        }

        StringBuilder fullResponse = new StringBuilder();

        return requestSpec.stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    addToHistory(sessionId, "assistant", fullResponse.toString());
                    log.debug("Streaming response completed: {} chars", fullResponse.length());
                })
                .doOnError(e -> {
                    log.error("Streaming error: {}", e.getMessage());
                    addToHistory(sessionId, "assistant",
                            "I apologize, but I encountered an error. Please try again.");
                });
    }

    /**
     * Clear conversation history for a session.
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("Cleared conversation history for session: {}", sessionId);
    }

    /**
     * Get the current conversation history for a session.
     */
    public List<ChatMessage> getHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, List.of());
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private String loadSystemPrompt() {
        try {
            return templateEngine.render(TEMPLATE_NAME, Map.of());
        } catch (Exception e) {
            log.warn("Failed to load chat_assistant template, using inline fallback");
            return """
                    You are an AI operations assistant for Amazon sellers. You help sellers manage their
                    day-to-day operations by answering questions, retrieving business data, and executing tasks
                    through your available tools. Be concise, data-driven, and proactive in identifying issues.
                    Use markdown formatting for clarity. When you see potential problems, alert the seller
                    immediately. Never suggest actions that violate Amazon's Terms of Service.
                    """;
        }
    }

    private void addToHistory(String sessionId, String role, String content) {
        conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(new ChatMessage(role, content));

        // Trim history to max size
        List<ChatMessage> history = conversationHistory.get(sessionId);
        while (history.size() > MAX_HISTORY_MESSAGES) {
            history.remove(0);
        }
    }

    private void trackUsage(ChatResponse response) {
        try {
            var metadata = response.getMetadata();
            if (metadata != null && metadata.getUsage() != null) {
                tokenTracker.track(AiTaskType.CHAT_ASSISTANT,
                        modelRouter.resolveModel(AiTaskType.CHAT_ASSISTANT),
                        (int) metadata.getUsage().getPromptTokens(),
                        (int) metadata.getUsage().getCompletionTokens());
            }
        } catch (Exception e) {
            log.warn("Failed to track token usage: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // DTOs
    // -----------------------------------------------------------------------

    public record ChatMessage(String role, String content) {}
}
