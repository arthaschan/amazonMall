package com.yudao.module.amazon.report.service;

import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.DashboardRespVO;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonDashboardMetricDO;
import cn.iocoder.yudao.module.amazon.report.dal.mysql.AmazonDashboardMetricMapper;
import cn.iocoder.yudao.module.amazon.report.service.DashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link DashboardServiceImpl} 单元测试
 *
 * @author AmazonOps AI
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private AmazonDashboardMetricMapper metricMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // ---------- 辅助方法 ----------

    private AmazonDashboardMetricDO buildMetric(LocalDate date,
                                                BigDecimal sales,
                                                Integer orders,
                                                BigDecimal adSpend,
                                                BigDecimal profit,
                                                BigDecimal inventoryScore,
                                                BigDecimal avgRating) {
        AmazonDashboardMetricDO m = new AmazonDashboardMetricDO();
        m.setMetricDate(date);
        m.setTotalSales(sales);
        m.setTotalOrders(orders);
        m.setAdSpend(adSpend);
        m.setProfit(profit);
        m.setInventoryHealthScore(inventoryScore);
        m.setAvgRating(avgRating);
        return m;
    }

    // ---------- 测试用例 ----------

    @Test
    @DisplayName("正常数据 - 3 条指标，验证汇总、趋势点数量、库存/评分取最后一条")
    void testGetDashboard_NormalData() {
        Long shopId = 1L;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 3);

        List<AmazonDashboardMetricDO> metrics = List.of(
                buildMetric(LocalDate.of(2024, 1, 1), new BigDecimal("100.00"), 10, new BigDecimal("20.00"), new BigDecimal("30.00"), new BigDecimal("80"), new BigDecimal("4.2")),
                buildMetric(LocalDate.of(2024, 1, 2), new BigDecimal("200.00"), 20, new BigDecimal("40.00"), new BigDecimal("60.00"), new BigDecimal("85"), new BigDecimal("4.3")),
                buildMetric(LocalDate.of(2024, 1, 3), new BigDecimal("300.00"), 30, new BigDecimal("60.00"), new BigDecimal("90.00"), new BigDecimal("90"), new BigDecimal("4.5"))
        );

        when(metricMapper.selectByDateRange(shopId, start, end)).thenReturn(metrics);

        DashboardRespVO resp = dashboardService.getDashboard(shopId, start, end);

        // 汇总验证
        assertThat(resp.getTotalSales()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(resp.getTotalOrders()).isEqualTo(60);
        assertThat(resp.getAdSpend()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(resp.getProfit()).isEqualByComparingTo(new BigDecimal("180.00"));

        // 趋势点
        assertThat(resp.getTrends()).hasSize(3);
        assertThat(resp.getTrends().get(0).getDate()).isEqualTo("2024-01-01");
        assertThat(resp.getTrends().get(0).getSales()).isEqualByComparingTo(new BigDecimal("100.00"));

        // 库存/评分取最后一条
        assertThat(resp.getInventoryHealthScore()).isEqualByComparingTo(new BigDecimal("90"));
        assertThat(resp.getAvgRating()).isEqualByComparingTo(new BigDecimal("4.5"));
    }

    @Test
    @DisplayName("空列表 - 所有汇总为零，无趋势，库存/评分为 null")
    void testGetDashboard_EmptyMetrics() {
        Long shopId = 1L;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 3);

        when(metricMapper.selectByDateRange(shopId, start, end)).thenReturn(Collections.emptyList());

        DashboardRespVO resp = dashboardService.getDashboard(shopId, start, end);

        assertThat(resp.getTotalSales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getTotalOrders()).isZero();
        assertThat(resp.getAdSpend()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getTrends()).isEmpty();
        assertThat(resp.getInventoryHealthScore()).isNull();
        assertThat(resp.getAvgRating()).isNull();
    }

    @Test
    @DisplayName("部分字段为 null - 跳过 null 安全累加")
    void testGetDashboard_NullFields() {
        Long shopId = 1L;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 2);

        List<AmazonDashboardMetricDO> metrics = List.of(
                buildMetric(LocalDate.of(2024, 1, 1), new BigDecimal("100.00"), null, null, new BigDecimal("30.00"), null, null),
                buildMetric(LocalDate.of(2024, 1, 2), null, 20, new BigDecimal("40.00"), null, null, null)
        );

        when(metricMapper.selectByDateRange(shopId, start, end)).thenReturn(metrics);

        DashboardRespVO resp = dashboardService.getDashboard(shopId, start, end);

        // sales: 100 + null → 100
        assertThat(resp.getTotalSales()).isEqualByComparingTo(new BigDecimal("100.00"));
        // orders: null + 20 → 20
        assertThat(resp.getTotalOrders()).isEqualTo(20);
        // adSpend: null + 40 → 40
        assertThat(resp.getAdSpend()).isEqualByComparingTo(new BigDecimal("40.00"));
        // profit: 30 + null → 30
        assertThat(resp.getProfit()).isEqualByComparingTo(new BigDecimal("30.00"));

        // 趋势点数量仍然为 2
        assertThat(resp.getTrends()).hasSize(2);

        // 最后一条的 inventory/rating 均为 null
        assertThat(resp.getInventoryHealthScore()).isNull();
        assertThat(resp.getAvgRating()).isNull();
    }

    @Test
    @DisplayName("单条指标 - 库存/评分来自该唯一指标")
    void testGetDashboard_SingleMetric() {
        Long shopId = 1L;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 1);

        List<AmazonDashboardMetricDO> metrics = List.of(
                buildMetric(LocalDate.of(2024, 1, 1), new BigDecimal("50.00"), 5, new BigDecimal("10.00"), new BigDecimal("15.00"), new BigDecimal("75"), new BigDecimal("4.0"))
        );

        when(metricMapper.selectByDateRange(shopId, start, end)).thenReturn(metrics);

        DashboardRespVO resp = dashboardService.getDashboard(shopId, start, end);

        assertThat(resp.getTotalSales()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(resp.getTotalOrders()).isEqualTo(5);
        assertThat(resp.getAdSpend()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(resp.getProfit()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(resp.getTrends()).hasSize(1);
        assertThat(resp.getInventoryHealthScore()).isEqualByComparingTo(new BigDecimal("75"));
        assertThat(resp.getAvgRating()).isEqualByComparingTo(new BigDecimal("4.0"));
    }
}
