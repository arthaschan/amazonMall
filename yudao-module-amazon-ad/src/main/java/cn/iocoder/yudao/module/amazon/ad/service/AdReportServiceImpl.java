package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdReportPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdReportDailyMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 广告报表服务实现。
 * <p>提供广告报表分页查询和报表数据同步功能。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AdReportServiceImpl implements AdReportService {

    @Resource
    private AmazonAdReportDailyMapper reportMapper;

    @Resource
    private AdReportSyncService adReportSyncService;

    @Resource
    private AmazonShopService amazonShopService;

    @Override
    public PageResult<AmazonAdReportDailyDO> getReportPage(AdReportPageReqVO reqVO) {
        return reportMapper.selectPage(reqVO);
    }

    @Override
    public void syncReports(Long shopId, Long campaignId) {
        // 1. 加载店铺信息
        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在, shopId=" + shopId);
        }

        // 2. 调用同步服务拉取广告报表
        adReportSyncService.syncAdReports(shopId, shop.getMarketplaceId());

        log.info("[syncReports] 广告报表同步完成, shopId={}, campaignId={}", shopId, campaignId);
    }
}
