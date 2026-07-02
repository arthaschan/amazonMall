package cn.iocoder.yudao.module.amazon.report.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.WeeklyReportPageReqVO;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonWeeklyReportDO;

/**
 * AI 周报 Service。
 * <p>自动汇总店铺经营数据，生成 AI 摘要和建议。</p>
 *
 * @author AmazonOps AI
 */
public interface AiWeeklyReportService {

    /**
     * 生成指定周的报告。
     *
     * @param shopId     店铺 ID
     * @param reportWeek 报告周 (如 2024-W03)
     * @return 生成的报告
     */
    AmazonWeeklyReportDO generateReport(Long shopId, String reportWeek);

    AmazonWeeklyReportDO getReport(Long shopId, String reportWeek);

    PageResult<AmazonWeeklyReportDO> getReportPage(WeeklyReportPageReqVO reqVO);
}
