package cn.iocoder.yudao.module.amazon.listing.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 产品保存 Request VO")
@Data
public class ProductSaveReqVO {

    @Schema(description = "产品 ID（更新时必填）")
    private Long id;

    @Schema(description = "店铺 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "店铺不能为空")
    private Long shopId;

    @Schema(description = "ASIN", requiredMode = Schema.RequiredMode.REQUIRED, example = "B08XXXXXXX")
    @NotBlank(message = "ASIN 不能为空")
    private String asin;

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "站点 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "站点不能为空")
    private String marketplaceId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "主图 URL")
    private String mainImageUrl;

    @Schema(description = "五点描述")
    private List<String> bulletPoints;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "后台关键词")
    private String backendKeywords;
}
