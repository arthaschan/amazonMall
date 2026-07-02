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
 * 广告活动数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_ad_campaign")
public class AmazonAdCampaignDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    /** 亚马逊广告活动 ID */
    private Long campaignId;

    private String campaignName;

    /** 广告类型：SP / SB / SD */
    private String campaignType;

    /** 投放类型：MANUAL / AUTO */
    private String targetingType;

    private BigDecimal dailyBudget;

    /** 状态：enabled / paused / archived */
    private String status;

    private LocalDate startDate;
    private LocalDate endDate;
}
