package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewPageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;

import java.util.List;

/**
 * 评论管理 Service。
 *
 * @author AmazonOps AI
 */
public interface ReviewService {

    PageResult<AmazonReviewDO> getReviewPage(ReviewPageReqVO reqVO);

    List<AmazonReviewDO> getByAsin(Long shopId, String asin);

    void syncReviews(Long shopId, String asin);
}
