package cn.iocoder.yudao.module.amazon.review.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewPageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonReviewMapper extends BaseMapperX<AmazonReviewDO> {

    default PageResult<AmazonReviewDO> selectPage(ReviewPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonReviewDO>()
                .eqIfPresent(AmazonReviewDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonReviewDO::getAsin, reqVO.getAsin())
                .eqIfPresent(AmazonReviewDO::getRating, reqVO.getRating())
                .eqIfPresent(AmazonReviewDO::getAiSentiment, reqVO.getAiSentiment())
                .orderByDesc(AmazonReviewDO::getReviewDate));
    }

    default List<AmazonReviewDO> selectByAsin(Long shopId, String asin) {
        return selectList(new LambdaQueryWrapperX<AmazonReviewDO>()
                .eq(AmazonReviewDO::getShopId, shopId)
                .eq(AmazonReviewDO::getAsin, asin));
    }
}
