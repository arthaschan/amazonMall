package cn.iocoder.yudao.module.amazon.report.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonDashboardMetricDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AmazonDashboardMetricMapper extends BaseMapperX<AmazonDashboardMetricDO> {

    default List<AmazonDashboardMetricDO> selectByDateRange(Long shopId, LocalDate start, LocalDate end) {
        return selectList(new LambdaQueryWrapperX<AmazonDashboardMetricDO>()
                .eq(AmazonDashboardMetricDO::getShopId, shopId)
                .between(AmazonDashboardMetricDO::getMetricDate, start, end)
                .orderByAsc(AmazonDashboardMetricDO::getMetricDate));
    }

    default AmazonDashboardMetricDO selectByDate(Long shopId, LocalDate date) {
        return selectOne(new LambdaQueryWrapperX<AmazonDashboardMetricDO>()
                .eq(AmazonDashboardMetricDO::getShopId, shopId)
                .eq(AmazonDashboardMetricDO::getMetricDate, date));
    }
}
