package cn.iocoder.yudao.module.amazon.report.service;

import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.DashboardRespVO;

import java.time.LocalDate;

/**
 * 仪表盘 Service。
 * <p>聚合各模块数据，提供经营概览。</p>
 *
 * @author AmazonOps AI
 */
public interface DashboardService {

    /**
     * 获取仪表盘数据。
     *
     * @param shopId 店铺 ID
     * @param start  开始日期
     * @param end    结束日期
     * @return 仪表盘数据
     */
    DashboardRespVO getDashboard(Long shopId, LocalDate start, LocalDate end);
}
