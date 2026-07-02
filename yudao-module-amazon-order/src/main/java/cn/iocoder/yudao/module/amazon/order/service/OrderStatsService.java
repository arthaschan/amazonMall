package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderStatsVO;

import java.time.LocalDate;

/**
 * 订单统计 Service。
 *
 * @author AmazonOps AI
 */
public interface OrderStatsService {

    /**
     * 获取指定店铺在时间范围内的订单统计。
     *
     * @param shopId 店铺 ID
     * @param start  开始日期
     * @param end    结束日期
     * @return 统计数据
     */
    OrderStatsVO getStats(Long shopId, LocalDate start, LocalDate end);
}
