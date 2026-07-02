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
 * 广告日报数据。
 * <p>按关键词粒度记录每日广告表现。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_ad_report_daily")
public class AmazonAdReportDailyDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;
    private Long campaignId;
    private Long adGroupId;
    private Long keywordId;

    private String keywordText;

    /** 匹配类型：EXACT / PHRASE / BROAD */
    private String matchType;

    private LocalDate reportDate;

    private Long impressions;
    private Long clicks;
    private BigDecimal cost;
    private BigDecimal sales;
    private Integer orders;
    private BigDecimal acos;
    private BigDecimal roas;
    private BigDecimal cpc;
    private BigDecimal ctr;
}
