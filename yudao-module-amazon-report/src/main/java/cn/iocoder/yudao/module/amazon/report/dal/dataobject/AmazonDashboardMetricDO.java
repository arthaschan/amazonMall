package cn.iocoder.yudao.module.amazon.report.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 仪表盘每日指标数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_dashboard_metric")
public class AmazonDashboardMetricDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    /** 指标日期 */
    private LocalDate metricDate;

    /** 总销售额 */
    private BigDecimal totalSales;

    /** 总订单数 */
    private Integer totalOrders;

    /** 广告花费 */
    private BigDecimal adSpend;

    /** 利润 */
    private BigDecimal profit;

    /** 库存健康评分 0-100 */
    private BigDecimal inventoryHealthScore;

    /** 平均评分 */
    private BigDecimal avgRating;
}
