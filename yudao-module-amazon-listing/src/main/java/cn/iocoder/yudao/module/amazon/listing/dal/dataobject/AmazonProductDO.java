package cn.iocoder.yudao.module.amazon.listing.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 亚马逊产品 Listing 数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_product", autoResultMap = true)
public class AmazonProductDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    @TableField("asin")
    private String asin;
    private String sku;
    private String marketplaceId;

    private String title;
    private String brand;
    private String categoryId;
    private BigDecimal price;
    private String currency;
    private String mainImageUrl;

    /** 五点描述 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> bulletPoints;

    private String description;
    private String backendKeywords;

    /** Listing 状态：ACTIVE / INACTIVE / SUPPRESSED */
    private String listingStatus;
    private Integer bsrRank;
    private BigDecimal rating;
    private Integer reviewCount;

    /** AI Listing 评分 0-100 */
    private BigDecimal aiListingScore;
}
