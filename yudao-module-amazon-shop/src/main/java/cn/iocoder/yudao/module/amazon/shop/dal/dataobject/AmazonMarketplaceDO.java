package cn.iocoder.yudao.module.amazon.shop.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Amazon marketplace configuration DO.
 *
 * <p>Reference data table storing the static configuration for each
 * Amazon marketplace (endpoints, currencies, region codes).
 * This table is typically seeded at deployment time and rarely modified.
 *
 * @author AmazonOps AI
 */
@Data
@TableName("amazon_marketplace")
@KeySequence("amazon_marketplace_id")
public class AmazonMarketplaceDO implements Serializable {

    /** Primary key. */
    @TableId
    private Long id;

    /** Amazon marketplace ID (e.g. ATVPDKIKX0DER for US). */
    private String marketplaceId;

    /** Region code: NA, EU, or FE. */
    private String region;

    /** ISO 3166-1 alpha-2 country code. */
    private String country;

    /** Marketplace display name (e.g. "Amazon.com", "Amazon.de"). */
    private String name;

    /** ISO 4217 currency code (e.g. USD, EUR, GBP). */
    private String currencyCode;

    /** SP-API regional endpoint URL. */
    private String endpointUrl;

    /** Creation time. */
    private LocalDateTime createTime;
}
