package cn.iocoder.yudao.module.amazon.review.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 评论 Response VO")
@Data
public class ReviewRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "评论 ID")
    private String reviewId;

    @Schema(description = "评论者")
    private String reviewerName;

    @Schema(description = "星级")
    private Integer rating;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String body;

    @Schema(description = "评论日期")
    private LocalDateTime reviewDate;

    @Schema(description = "认证购买")
    private Boolean verifiedPurchase;

    @Schema(description = "有用票数")
    private Integer helpfulVotes;

    @Schema(description = "AI 情感")
    private String aiSentiment;

    @Schema(description = "AI 主题")
    private List<String> aiTopics;

    @Schema(description = "AI 痛点")
    private List<String> aiPainPoints;

    @Schema(description = "AI 卖点")
    private List<String> aiSellingPoints;
}
