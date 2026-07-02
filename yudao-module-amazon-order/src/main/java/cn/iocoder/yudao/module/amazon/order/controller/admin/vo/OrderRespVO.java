package cn.iocoder.yudao.module.amazon.order.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 订单 Response VO")
@Data
public class OrderRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "亚马逊订单号")
    private String amazonOrderId;

    @Schema(description = "站点 ID")
    private String marketplaceId;

    @Schema(description = "订单状态")
    private String orderStatus;

    @Schema(description = "订单总额")
    private BigDecimal orderTotal;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "下单时间")
    private LocalDateTime purchaseDate;

    @Schema(description = "配送渠道")
    private String fulfillmentChannel;

    @Schema(description = "是否企业订单")
    private Boolean isBusinessOrder;

    @Schema(description = "是否 Prime")
    private Boolean isPrime;

    @Schema(description = "收货城市")
    private String shipCity;

    @Schema(description = "收货州/省")
    private String shipState;

    @Schema(description = "收货国家")
    private String shipCountry;

    @Schema(description = "商品数量")
    private Integer itemCount;

    @Schema(description = "订单明细")
    private List<OrderItemVO> items;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Data
    @Schema(description = "订单明细 VO")
    public static class OrderItemVO {
        @Schema(description = "ASIN")
        private String asin;
        @Schema(description = "SKU")
        private String sku;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "数量")
        private Integer quantity;
        @Schema(description = "单价")
        private BigDecimal price;
        @Schema(description = "币种")
        private String currency;
    }
}
