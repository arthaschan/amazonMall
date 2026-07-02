package cn.iocoder.yudao.module.amazon.ai.core.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistent record of a single AI API call's token usage.
 *
 * <p>Stored in the {@code amazon_ai_token_usage} table.
 * Used for monthly budget tracking, cost attribution, and alerting.
 *
 * @author AmazonOps AI
 */
@Data
@TableName("amazon_ai_token_usage")
@KeySequence("amazon_ai_token_usage_seq")
public class AiTokenUsageDO {

    /** Primary key */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Tenant ID for multi-tenancy isolation */
    private Long tenantId;

    /** The AI task type that triggered this usage (e.g. "listing_generation") */
    private String taskType;

    /** Model used (e.g. "gpt-4o", "gpt-4o-mini") */
    private String model;

    /** Number of input (prompt) tokens consumed */
    private Integer inputTokens;

    /** Number of output (completion) tokens consumed */
    private Integer outputTokens;

    /** Total tokens = input + output */
    private Integer totalTokens;

    /** Estimated cost in USD (calculated from model pricing) */
    private Double estimatedCostUsd;

    /** Optional: the business entity ID that triggered this (e.g. listing ID, ASIN) */
    private String businessId;

    /** Optional: the user ID who initiated the request */
    private Long userId;

    /** Year-month for aggregation queries, e.g. "2025-06" */
    private String yearMonth;

    /** Creation time */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
