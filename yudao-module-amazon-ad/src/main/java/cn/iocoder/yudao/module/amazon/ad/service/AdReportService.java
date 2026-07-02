package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdReportPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;

/**
 * 广告报表 Service。
 *
 * @author AmazonOps AI
 */
public interface AdReportService {

    PageResult<AmazonAdReportDailyDO> getReportPage(AdReportPageReqVO reqVO);

    void syncReports(Long shopId, Long campaignId);
}
