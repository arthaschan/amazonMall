package cn.iocoder.yudao.module.amazon.inventory.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存预测数据对象。
 * <p>基于历史销量和趋势预测未来库存需求。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_inventory_forecast")
public class AmazonInventoryForecastDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;
    private String asin;

    /** 预测日期 */
    private LocalDate forecastDate;

    /** 预测日均销量 */
    private BigDecimal predictedDailySales;

    /** 置信度 0-1 */
    private BigDecimal confidence;

    /** 再订购点 */
    private Integer reorderPoint;

    /** 安全库存 */
    private Integer safetyStock;

    /** 建议补货量 */
    private Integer suggestedReorderQty;

    /** 备货周期（天） */
    private Integer leadTimeDays;

    /** 预测生成日期 */
    private LocalDate generateDate;
}
