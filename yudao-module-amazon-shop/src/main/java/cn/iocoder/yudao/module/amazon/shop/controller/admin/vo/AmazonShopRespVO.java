package cn.iocoder.yudao.module.amazon.shop.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Management Console - Amazon Shop Response VO")
@Data
public class AmazonShopRespVO {

    @Schema(description = "Shop ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "Tenant ID", example = "1")
    private Long tenantId;

    @Schema(description = "Shop display name", requiredMode = Schema.RequiredMode.REQUIRED, example = "My US Store")
    private String shopName;

    @Schema(description = "Amazon marketplace ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "ATVPDKIKX0DER")
    private String marketplaceId;

    @Schema(description = "ISO country code", requiredMode = Schema.RequiredMode.REQUIRED, example = "US")
    private String countryCode;

    @Schema(description = "Amazon seller ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "A2EXAMPLE123")
    private String sellerId;

    @Schema(description = "AWS IAM Role ARN", example = "arn:aws:iam::123456789012:role/SPApiRole")
    private String iamArn;

    @Schema(description = "Shop status (0=disabled, 1=enabled, 2=auth_expired, 3=auth_pending)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "Access token expiry time")
    private LocalDateTime tokenExpireAt;

    @Schema(description = "Whether credentials are configured (tokens present)", example = "true")
    private Boolean credentialConfigured;

    @Schema(description = "Creation time", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "Last update time")
    private LocalDateTime updateTime;

    // NOTE: clientId, clientSecret, refreshToken, accessToken are NEVER
    // exposed in the response VO for security reasons.
}
