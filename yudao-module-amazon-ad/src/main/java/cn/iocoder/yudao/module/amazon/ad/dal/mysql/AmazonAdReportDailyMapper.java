package cn.iocoder.yudao.module.amazon.ad.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdReportPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonAdReportDailyMapper extends BaseMapperX<AmazonAdReportDailyDO> {

    default PageResult<AmazonAdReportDailyDO> selectPage(AdReportPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonAdReportDailyDO>()
                .eqIfPresent(AmazonAdReportDailyDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonAdReportDailyDO::getCampaignId, reqVO.getCampaignId())
                .likeIfPresent(AmazonAdReportDailyDO::getKeywordText, reqVO.getKeywordText())
                .betweenIfPresent(AmazonAdReportDailyDO::getReportDate, reqVO.getReportDateStart(), reqVO.getReportDateEnd())
                .orderByDesc(AmazonAdReportDailyDO::getReportDate));
    }
}
