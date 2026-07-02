package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderStatsVO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class OrderStatsServiceImpl implements OrderStatsService {

    @Resource
    private AmazonOrderMapper orderMapper;

    @Override
    public OrderStatsVO getStats(Long shopId, LocalDate start, LocalDate end) {
        // TODO: 实际聚合查询
        var stats = new OrderStatsVO();
        stats.setTotalOrders(0);
        stats.setTotalSales(BigDecimal.ZERO);
        stats.setAvgOrderValue(BigDecimal.ZERO);
        stats.setFbaOrderRate(BigDecimal.ZERO);
        stats.setRefundOrders(0);
        return stats;
    }
}
