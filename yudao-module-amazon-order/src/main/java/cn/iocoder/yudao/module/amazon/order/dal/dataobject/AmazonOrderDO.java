package cn.iocoder.yudao.module.amazon.order.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 亚马逊订单数据对象。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_order")
public class AmazonOrderDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    /** 亚马逊订单号 */
    private String amazonOrderId;

    private String marketplaceId;

    /** 订单状态：Pending / Unshipped / PartiallyShipped / Shipped / Canceled / Unfulfillable */
    private String orderStatus;

    private BigDecimal orderTotal;
    private String currency;
    private LocalDateTime purchaseDate;

    /** 配送渠道：AFN / MFN */
    private String fulfillmentChannel;

    private Boolean isBusinessOrder;
    private Boolean isPrime;

    private String shipCity;
    private String shipState;
    private String shipCountry;
    private Integer itemCount;
}
