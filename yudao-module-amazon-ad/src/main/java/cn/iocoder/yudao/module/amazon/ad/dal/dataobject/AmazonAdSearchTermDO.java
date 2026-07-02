package cn.iocoder.yudao.module.amazon.ad.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告搜索词报告数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_ad_search_term")
public class AmazonAdSearchTermDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;
    private Long campaignId;

    /** 用户实际搜索词 */
    private String searchTerm;

    private LocalDate reportDate;

    private Long impressions;
    private Long clicks;
    private BigDecimal cost;
    private BigDecimal sales;
    private Integer orders;

    /** AI 标签：OPPORTUNITY / WASTE / KEEP / NEGATIVE */
    private String aiTag;
}
