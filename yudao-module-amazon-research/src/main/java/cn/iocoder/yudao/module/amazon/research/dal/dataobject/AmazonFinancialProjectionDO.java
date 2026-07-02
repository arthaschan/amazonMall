package cn.iocoder.yudao.module.amazon.research.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务预测数据对象。
 * <p>包含 52 周的收入/成本/利润预测 JSON，用于评估产品机会的商业可行性。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_financial_projection", autoResultMap = true)
public class AmazonFinancialProjectionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联产品机会 ID */
    private Long opportunityId;

    /** 启动成本 */
    private BigDecimal startupCost;

    /** 回本周数 */
    private Integer breakEvenWeek;

    /** 12 个月累计利润 */
    private BigDecimal twelveMonthProfit;

    /** 品牌估值 */
    private BigDecimal brandValuation;

    /**
     * 52 周预测数据 JSON。
     * 每条记录包含 week / revenue / cost / profit / cashFlow 等字段。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<WeeklyProjection> projectionData;

    /**
     * 单周财务预测记录。
     */
    @Data
    public static class WeeklyProjection {
        private Integer week;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal cashFlow;
        private BigDecimal cumulativeProfit;
    }
}
