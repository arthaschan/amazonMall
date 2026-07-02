package cn.iocoder.yudao.module.amazon.review.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 评论分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewPageReqVO extends PageParam {

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "星级 1-5")
    private Integer rating;

    @Schema(description = "AI 情感: POSITIVE / NEUTRAL / NEGATIVE")
    private String aiSentiment;
}
