package cn.iocoder.yudao.module.amazon.shop.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonShopStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Amazon shop/seller account DO.
 *
 * <p>Represents a single Amazon seller account bound to a specific marketplace.
 * Sensitive credentials (client_id, client_secret, refresh_token, access_token)
 * are encrypted with AES-256-GCM before persistence.
 *
 * @see AmazonShopStatusEnum for status codes
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_shop")
@KeySequence("amazon_shop_id")
public class AmazonShopDO extends BaseDO {

    /** Primary key. */
    @TableId
    private Long id;

    /** Tenant ID for multi-tenant isolation. */
    private Long tenantId;

    /** Shop display name (operator-defined). */
    private String shopName;

    /** Amazon marketplace ID (e.g. ATVPDKIKX0DER for US). */
    private String marketplaceId;

    /** ISO 3166-1 alpha-2 country code (e.g. US, UK, DE). */
    private String countryCode;

    /** Amazon seller ID (from Seller Central). */
    private String sellerId;

    /** LWA client ID (encrypted AES-256-GCM). */
    private String clientId;

    /** LWA client secret (encrypted AES-256-GCM). */
    private String clientSecret;

    /** SP-API refresh token (encrypted AES-256-GCM, long-lived). */
    private String refreshToken;

    /** SP-API access token (encrypted AES-256-GCM, short-lived cache). */
    private String accessToken;

    /** Access token expiry timestamp. */
    private LocalDateTime tokenExpireAt;

    /** AWS IAM Role ARN used for SP-API request signing. */
    private String iamArn;

    /**
     * Shop status.
     *
     * @see AmazonShopStatusEnum
     * 0=DISABLED, 1=ENABLED, 2=AUTH_EXPIRED, 3=AUTH_PENDING
     */
    private Integer status;
}
