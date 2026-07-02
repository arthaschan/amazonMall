package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.CampaignPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdCampaignMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdCampaignServiceImpl implements AdCampaignService {

    @Resource
    private AmazonAdCampaignMapper campaignMapper;

    @Override
    public AmazonAdCampaignDO getCampaign(Long id) {
        return campaignMapper.selectById(id);
    }

    @Override
    public PageResult<AmazonAdCampaignDO> getCampaignPage(CampaignPageReqVO reqVO) {
        return campaignMapper.selectPage(reqVO);
    }

    @Override
    public void syncCampaigns(Long shopId) {
        // TODO: 调用 SP-API Advertising API 同步广告活动
    }
}
