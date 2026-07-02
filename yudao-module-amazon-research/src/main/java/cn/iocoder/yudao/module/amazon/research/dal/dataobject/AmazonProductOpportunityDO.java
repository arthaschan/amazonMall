package cn.iocoder.yudao.module.amazon.research.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 产品机会数据对象。
 * <p>关联 Niche，记录具体 ASIN 级别的市场机会数据。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_product_opportunity")
public class AmazonProductOpportunityDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联品类 ID */
    private Long nicheId;

    /** ASIN */
    private String asin;

    /** 商品标题 */
    private String title;

    /** 售价 */
    private BigDecimal price;

    /** 币种 */
    private String currency;

    /** 星级评分 */
    private BigDecimal rating;

    /** 评论数 */
    private Integer reviewCount;

    /** BSR 排名 */
    private Integer bsr;

    /** 月搜索量 */
    private Long monthlySearchVolume;

    /** 预估月销量 */
    private Integer estimatedMonthlySales;

    /** 利润率 */
    private BigDecimal profitMargin;
}
