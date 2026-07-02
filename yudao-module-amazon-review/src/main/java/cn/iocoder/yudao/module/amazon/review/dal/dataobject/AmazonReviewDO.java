package cn.iocoder.yudao.module.amazon.review.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 亚马逊评论数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_review", autoResultMap = true)
public class AmazonReviewDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;
    private String asin;

    /** 亚马逊评论 ID */
    private String reviewId;

    private String reviewerName;

    /** 星级 1-5 */
    private Integer rating;

    private String title;
    private String body;
    private LocalDateTime reviewDate;
    private Boolean verifiedPurchase;
    private Integer helpfulVotes;

    /** AI 情感分析: POSITIVE / NEUTRAL / NEGATIVE */
    private String aiSentiment;

    /** AI 提取的主题标签 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> aiTopics;

    /** AI 提取的痛点 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> aiPainPoints;

    /** AI 提取的卖点 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> aiSellingPoints;
}
