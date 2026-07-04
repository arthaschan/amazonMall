package cn.iocoder.yudao.module.amazon.report.service;

import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.DashboardRespVO;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonDashboardMetricDO;
import cn.iocoder.yudao.module.amazon.report.dal.mysql.AmazonDashboardMetricMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private AmazonDashboardMetricMapper metricMapper;

    @Override
    public DashboardRespVO getDashboard(Long shopId, LocalDate start, LocalDate end) {
        List<AmazonDashboardMetricDO> metrics = metricMapper.selectByDateRange(shopId, start, end);

        DashboardRespVO resp = new DashboardRespVO();
        BigDecimal totalSales = BigDecimal.ZERO;
        int totalOrders = 0;
        BigDecimal totalAdSpend = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        ArrayList<DashboardRespVO.TrendPoint> trends = new ArrayList<DashboardRespVO.TrendPoint>();

        for (AmazonDashboardMetricDO m : metrics) {
            if (m.getTotalSales() != null) totalSales = totalSales.add(m.getTotalSales());
            if (m.getTotalOrders() != null) totalOrders += m.getTotalOrders();
            if (m.getAdSpend() != null) totalAdSpend = totalAdSpend.add(m.getAdSpend());
            if (m.getProfit() != null) totalProfit = totalProfit.add(m.getProfit());

            DashboardRespVO.TrendPoint tp = new DashboardRespVO.TrendPoint();
            tp.setDate(m.getMetricDate().toString());
            tp.setSales(m.getTotalSales());
            tp.setOrders(m.getTotalOrders());
            tp.setProfit(m.getProfit());
            trends.add(tp);
        }

        resp.setTotalSales(totalSales);
        resp.setTotalOrders(totalOrders);
        resp.setAdSpend(totalAdSpend);
        resp.setProfit(totalProfit);
        resp.setTrends(trends);

        // 取最新的库存健康评分和平均评分
        if (!metrics.isEmpty()) {
            AmazonDashboardMetricDO latest = metrics.get(metrics.size() - 1);
            resp.setInventoryHealthScore(latest.getInventoryHealthScore());
            resp.setAvgRating(latest.getAvgRating());
        }

        return resp;
    }
}
