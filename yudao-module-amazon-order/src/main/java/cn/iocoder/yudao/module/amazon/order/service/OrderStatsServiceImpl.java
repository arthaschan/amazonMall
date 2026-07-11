package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderStatsVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 订单统计服务实现。
 *
 * <p>基于 amazon_order 表进行聚合统计，包括订单数、销售额、
 * 客单价、FBA 占比和退款订单数。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class OrderStatsServiceImpl implements OrderStatsService {

    @Resource
    private AmazonOrderMapper orderMapper;

    @Override
    public OrderStatsVO getStats(Long shopId, LocalDate start, LocalDate end) {
        log.info("[OrderStats] 查询订单统计 shopId={}, range=[{}, {}]", shopId, start, end);

        LocalDateTime startTime = (start != null) ? start.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime endTime = (end != null) ? end.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();

        // 查询时间范围内的有效订单（排除 Canceled）
        List<AmazonOrderDO> orders = orderMapper.selectList(
                new LambdaQueryWrapperX<AmazonOrderDO>()
                        .eq(AmazonOrderDO::getShopId, shopId)
                        .ge(AmazonOrderDO::getPurchaseDate, startTime)
                        .lt(AmazonOrderDO::getPurchaseDate, endTime)
                        .notIn(AmazonOrderDO::getOrderStatus,
                                Arrays.asList("Canceled", "Unfulfillable")));

        OrderStatsVO stats = new OrderStatsVO();

        if (orders.isEmpty()) {
            stats.setTotalOrders(0);
            stats.setTotalSales(BigDecimal.ZERO);
            stats.setAvgOrderValue(BigDecimal.ZERO);
            stats.setFbaOrderRate(BigDecimal.ZERO);
            stats.setRefundOrders(0);
            return stats;
        }

        int totalOrders = orders.size();
        BigDecimal totalSales = BigDecimal.ZERO;
        int fbaOrders = 0;
        int refundOrders = 0;

        for (AmazonOrderDO order : orders) {
            // 累加销售额
            if (order.getOrderTotal() != null) {
                totalSales = totalSales.add(order.getOrderTotal());
            }

            // FBA 订单计数（fulfillmentChannel = AFN）
            if ("AFN".equals(order.getFulfillmentChannel())) {
                fbaOrders++;
            }

            // 退款/取消订单计数
            String status = order.getOrderStatus();
            if (status != null && ("Refund".equalsIgnoreCase(status)
                    || "Unfulfillable".equalsIgnoreCase(status))) {
                refundOrders++;
            }
        }

        // 计算指标
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal fbaOrderRate = totalOrders > 0
                ? BigDecimal.valueOf(fbaOrders)
                        .divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        stats.setTotalOrders(totalOrders);
        stats.setTotalSales(totalSales.setScale(2, RoundingMode.HALF_UP));
        stats.setAvgOrderValue(avgOrderValue);
        stats.setFbaOrderRate(fbaOrderRate);
        stats.setRefundOrders(refundOrders);

        log.info("[OrderStats] 统计完成 shopId={}, totalOrders={}, totalSales={}, avgOrderValue={}, fbaRate={}%, refunds={}",
                shopId, totalOrders, totalSales, avgOrderValue, fbaOrderRate, refundOrders);

        return stats;
    }
}
