package cn.iocoder.yudao.module.amazon.shop.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Amazon SP-API call audit log DO.
 *
 * <p>Records every API request made to Amazon SP-API for monitoring,
 * debugging, rate-limit analysis, and compliance auditing.
 * Request parameters are sanitised to remove sensitive data before storage.
 *
 * @author AmazonOps AI
 */
@Data
@TableName("amazon_api_log")
@KeySequence("amazon_api_log_id")
public class AmazonApiLogDO implements Serializable {

    /** Primary key. */
    @TableId
    private Long id;

    /** FK to {@link AmazonShopDO#getId()}. */
    private Long shopId;

    /** Tenant ID for multi-tenant isolation. */
    private Long tenantId;

    /** API endpoint path (e.g. /orders/v0/orders). */
    private String apiEndpoint;

    /** HTTP method (GET, POST, PUT, DELETE). */
    private String requestMethod;

    /** Sanitized request parameters (no secrets or tokens). */
    private String requestParams;

    /** HTTP response status code (e.g. 200, 429, 500). */
    private Integer responseCode;

    /** Response time in milliseconds. */
    private Integer responseTimeMs;

    /** Rate limit remaining from x-amzn-RateLimit-Limit header. */
    private Double rateLimitRemaining;

    /** Error message if the request failed. */
    private String errorMessage;

    /** Request timestamp. */
    private LocalDateTime createTime;
}
