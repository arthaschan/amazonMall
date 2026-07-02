package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.CampaignPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;

/**
 * 广告活动管理 Service。
 *
 * @author AmazonOps AI
 */
public interface AdCampaignService {

    AmazonAdCampaignDO getCampaign(Long id);

    PageResult<AmazonAdCampaignDO> getCampaignPage(CampaignPageReqVO reqVO);

    void syncCampaigns(Long shopId);
}
