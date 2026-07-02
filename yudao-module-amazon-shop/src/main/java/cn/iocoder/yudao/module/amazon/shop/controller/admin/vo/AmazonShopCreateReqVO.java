package cn.iocoder.yudao.module.amazon.shop.controller.admin.vo;

import cn.iocoder.yudao.module.amazon.shop.enums.AmazonShopStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Management Console - Amazon Shop Create Request VO")
@Data
public class AmazonShopCreateReqVO {

    @Schema(description = "Shop display name", requiredMode = Schema.RequiredMode.REQUIRED, example = "My US Store")
    @NotBlank(message = "Shop name must not be blank")
    @Size(max = 100, message = "Shop name must not exceed 100 characters")
    private String shopName;

    @Schema(description = "Amazon marketplace ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "ATVPDKIKX0DER")
    @NotBlank(message = "Marketplace ID must not be blank")
    @Size(max = 32, message = "Marketplace ID must not exceed 32 characters")
    private String marketplaceId;

    @Schema(description = "ISO country code", requiredMode = Schema.RequiredMode.REQUIRED, example = "US")
    @NotBlank(message = "Country code must not be blank")
    @Size(max = 10, message = "Country code must not exceed 10 characters")
    private String countryCode;

    @Schema(description = "Amazon seller ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "A2EXAMPLE123")
    @NotBlank(message = "Seller ID must not be blank")
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

    @Schema(description = "Shop status", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "Status must not be null")
    private Integer status;
}
