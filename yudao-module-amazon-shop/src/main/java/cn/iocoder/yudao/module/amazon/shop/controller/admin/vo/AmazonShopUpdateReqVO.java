package cn.iocoder.yudao.module.amazon.shop.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Management Console - Amazon Shop Update Request VO")
@Data
public class AmazonShopUpdateReqVO {

    @Schema(description = "Shop ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "Shop ID must not be null")
    private Long id;

    @Schema(description = "Shop display name", example = "My US Store")
    @Size(max = 100, message = "Shop name must not exceed 100 characters")
    private String shopName;

    @Schema(description = "Amazon marketplace ID", example = "ATVPDKIKX0DER")
    @Size(max = 32, message = "Marketplace ID must not exceed 32 characters")
    private String marketplaceId;

    @Schema(description = "ISO country code", example = "US")
    @Size(max = 10, message = "Country code must not exceed 10 characters")
    private String countryCode;

    @Schema(description = "Amazon seller ID", example = "A2EXAMPLE123")
    @Size(max = 64, message = "Seller ID must not exceed 64 characters")
    private String sellerId;

    @Schema(description = "LWA client ID (plaintext, will be encrypted)", example = "amzn1.application-oa2-client.xxx")
    private String clientId;

    @Schema(description = "LWA client secret (plaintext, will be encrypted)", example = "amzn1.oa2-cs.v1.xxx")
    private String clientSecret;

    @Schema(description = "SP-API refresh token (plaintext, will be encrypted)", example = "Atza|xxx")
    private String refreshToken;

    @Schema(description = "AWS IAM Role ARN", example = "arn:aws:iam::123456789012:role/SPApiRole")
    @Size(max = 256, message = "IAM ARN must not exceed 256 characters")
    private String iamArn;

    @Schema(description = "Shop status (0=disabled, 1=enabled, 2=auth_expired, 3=auth_pending)", example = "1")
    private Integer status;
}
