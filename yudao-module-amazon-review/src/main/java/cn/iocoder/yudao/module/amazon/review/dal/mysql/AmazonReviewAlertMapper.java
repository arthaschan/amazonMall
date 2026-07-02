package cn.iocoder.yudao.module.amazon.review.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewAlertDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonReviewAlertMapper extends BaseMapperX<AmazonReviewAlertDO> {

    default PageResult<AmazonReviewAlertDO> selectPage(ReviewAlertPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonReviewAlertDO>()
                .eqIfPresent(AmazonReviewAlertDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonReviewAlertDO::getAsin, reqVO.getAsin())
                .eqIfPresent(AmazonReviewAlertDO::getAcknowledged, reqVO.getAcknowledged())
                .orderByDesc(AmazonReviewAlertDO::getAlertTime));
    }

    default List<AmazonReviewAlertDO> selectUnacknowledged(Long shopId) {
        return selectList(new LambdaQueryWrapperX<AmazonReviewAlertDO>()
                .eq(AmazonReviewAlertDO::getShopId, shopId)
                .eq(AmazonReviewAlertDO::getAcknowledged, false));
    }
}
