package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdReportPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdReportDailyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdReportServiceImpl implements AdReportService {

    @Resource
    private AmazonAdReportDailyMapper reportMapper;

    @Override
    public PageResult<AmazonAdReportDailyDO> getReportPage(AdReportPageReqVO reqVO) {
        return reportMapper.selectPage(reqVO);
    }

    @Override
    public void syncReports(Long shopId, Long campaignId) {
        // TODO: 调用 SP-API Advertising Reports API
    }
}
