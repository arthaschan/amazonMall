package cn.iocoder.yudao.module.amazon.ad.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.SearchTermPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdSearchTermDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonAdSearchTermMapper extends BaseMapperX<AmazonAdSearchTermDO> {

    default PageResult<AmazonAdSearchTermDO> selectPage(SearchTermPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonAdSearchTermDO>()
                .eqIfPresent(AmazonAdSearchTermDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonAdSearchTermDO::getCampaignId, reqVO.getCampaignId())
                .likeIfPresent(AmazonAdSearchTermDO::getSearchTerm, reqVO.getSearchTerm())
                .eqIfPresent(AmazonAdSearchTermDO::getAiTag, reqVO.getAiTag())
                .betweenIfPresent(AmazonAdSearchTermDO::getReportDate, reqVO.getReportDateStart(), reqVO.getReportDateEnd())
                .orderByDesc(AmazonAdSearchTermDO::getClicks));
    }
}
