package cn.iocoder.yudao.module.amazon.order.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * FBA 货件数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_fba_shipment")
public class AmazonFbaShipmentDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    /** 亚马逊货件 ID */
    private String shipmentId;

    private String shipmentName;

    /** 货件状态：WORKING / SHIPPED / IN_TRANSIT / DELIVERED / CHECKED_IN / RECEIVING / CLOSED / CANCELLED */
    private String status;

    /** 目的仓库代码 */
    private String destinationFc;

    /** 发货地址 */
    private String shipFromAddress;

    /** 标签数量 */
    private Integer labelCount;
}
