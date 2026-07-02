package cn.iocoder.yudao.module.amazon.shop.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Management Console - Amazon Shop Page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AmazonShopPageReqVO extends PageParam {

    @Schema(description = "Shop name (fuzzy match)", example = "My Store")
    private String shopName;

    @Schema(description = "Amazon marketplace ID (exact match)", example = "ATVPDKIKX0DER")
    private String marketplaceId;

    @Schema(description = "Amazon seller ID (exact match)", example = "A2EXAMPLE123")
    private String sellerId;

    @Schema(description = "ISO country code (exact match)", example = "US")
    private String countryCode;

    @Schema(description = "Shop status (exact match): 0=disabled, 1=enabled, 2=auth_expired, 3=auth_pending",
            example = "1")
    private Integer status;

    @Schema(description = "Creation time range start")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeStart;

    @Schema(description = "Creation time range end")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeEnd;
}
