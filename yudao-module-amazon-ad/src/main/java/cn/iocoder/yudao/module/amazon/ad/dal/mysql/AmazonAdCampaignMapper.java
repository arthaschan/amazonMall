package cn.iocoder.yudao.module.amazon.ad.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.CampaignPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonAdCampaignMapper extends BaseMapperX<AmazonAdCampaignDO> {

    default PageResult<AmazonAdCampaignDO> selectPage(CampaignPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonAdCampaignDO>()
                .likeIfPresent(AmazonAdCampaignDO::getCampaignName, reqVO.getCampaignName())
                .eqIfPresent(AmazonAdCampaignDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonAdCampaignDO::getCampaignType, reqVO.getCampaignType())
                .eqIfPresent(AmazonAdCampaignDO::getStatus, reqVO.getStatus())
                .orderByDesc(AmazonAdCampaignDO::getId));
    }
}
