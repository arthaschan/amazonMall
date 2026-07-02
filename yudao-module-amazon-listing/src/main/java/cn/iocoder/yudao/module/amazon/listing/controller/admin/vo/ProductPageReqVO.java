package cn.iocoder.yudao.module.amazon.listing.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 产品分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageReqVO extends PageParam {

    @Schema(description = "ASIN", example = "B08XXXXXXX")
    private String asin;

    @Schema(description = "标题关键词")
    private String title;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "站点 ID")
    private String marketplaceId;

    @Schema(description = "Listing 状态")
    private String listingStatus;
}
