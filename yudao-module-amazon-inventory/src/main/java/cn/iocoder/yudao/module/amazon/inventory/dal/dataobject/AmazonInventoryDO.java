package cn.iocoder.yudao.module.amazon.inventory.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * FBA 库存快照数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_inventory")
public class AmazonInventoryDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    private String asin;
    private String sku;

    /** 仓库代码 */
    private String fulfillmentCenter;

    /** 可售数量 */
    private Integer availableQty;

    /** 预留数量 */
    private Integer reservedQty;

    /** 入库中数量 */
    private Integer inboundQty;

    /** 不可售数量 */
    private Integer unfulfillableQty;

    /** 可售天数 */
    private Integer daysOfSupply;

    /** 快照日期 */
    private LocalDate snapshotDate;
}
