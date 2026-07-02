package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewAlertDO;

import java.util.List;

/**
 * 评论预警 Service。
 *
 * @author AmazonOps AI
 */
public interface ReviewAlertService {

    PageResult<AmazonReviewAlertDO> getAlertPage(ReviewAlertPageReqVO reqVO);

    List<AmazonReviewAlertDO> getUnacknowledgedAlerts(Long shopId);

    void acknowledgeAlert(Long alertId);
}
