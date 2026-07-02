package cn.iocoder.yudao.module.amazon.report.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 周度经营报告数据对象。
 * <p>每周自动汇总店铺核心指标，并生成 AI 摘要和建议。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_weekly_report", autoResultMap = true)
public class AmazonWeeklyReportDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    /** 报告所属周 (ISO 格式: 2024-W03) */
    private String reportWeek;

    /** 报告数据 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ReportData reportData;

    /** AI 摘要 */
    private String aiSummary;

    /** AI 建议列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> aiRecommendations;

    /** 状态: 0=生成中, 1=已完成 */
    private Integer status;

    @Data
    public static class ReportData {
        private Double totalSales;
        private Integer totalOrders;
        private Double adSpend;
        private Double profit;
        private Double conversionRate;
        private Integer newReviews;
        private Double avgRating;
        private Integer inventoryAlerts;
    }
}
