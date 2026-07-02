package cn.iocoder.yudao.module.amazon.report.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.WeeklyReportPageReqVO;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonWeeklyReportDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonWeeklyReportMapper extends BaseMapperX<AmazonWeeklyReportDO> {

    default PageResult<AmazonWeeklyReportDO> selectPage(WeeklyReportPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonWeeklyReportDO>()
                .eqIfPresent(AmazonWeeklyReportDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonWeeklyReportDO::getReportWeek, reqVO.getReportWeek())
                .orderByDesc(AmazonWeeklyReportDO::getReportWeek));
    }

    default AmazonWeeklyReportDO selectByWeek(Long shopId, String reportWeek) {
        return selectOne(new LambdaQueryWrapperX<AmazonWeeklyReportDO>()
                .eq(AmazonWeeklyReportDO::getShopId, shopId)
                .eq(AmazonWeeklyReportDO::getReportWeek, reportWeek));
    }
}
