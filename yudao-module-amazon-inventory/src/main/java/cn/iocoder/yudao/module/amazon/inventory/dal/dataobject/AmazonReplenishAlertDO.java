package cn.iocoder.yudao.module.amazon.inventory.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 补货预警数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_replenish_alert")
public class AmazonReplenishAlertDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;
    private String asin;

    /** 预警类型：LOW_STOCK / OUT_OF_STOCK / OVERSTOCK */
    private String alertType;

    /** 当前库存 */
    private Integer currentQty;

    /** 再订购点 */
    private Integer reorderPoint;

    /** 建议补货量 */
    private Integer suggestedQty;

    /** 是否已确认 */
    private Boolean acknowledged;
}
