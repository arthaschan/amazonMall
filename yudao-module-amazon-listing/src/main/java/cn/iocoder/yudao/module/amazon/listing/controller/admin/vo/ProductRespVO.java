package cn.iocoder.yudao.module.amazon.listing.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 产品 Response VO")
@Data
public class ProductRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "站点 ID")
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

    @Schema(description = "Listing 状态")
    private String listingStatus;

    @Schema(description = "BSR 排名")
    private Integer bsrRank;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "评论数")
    private Integer reviewCount;

    @Schema(description = "AI Listing 评分")
    private BigDecimal aiListingScore;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
