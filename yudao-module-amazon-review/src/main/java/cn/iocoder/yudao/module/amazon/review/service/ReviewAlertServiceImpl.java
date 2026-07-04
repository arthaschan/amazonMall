package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewAlertDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewAlertMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewAlertServiceImpl implements ReviewAlertService {

    @Resource
    private AmazonReviewAlertMapper alertMapper;

    @Override
    public PageResult<AmazonReviewAlertDO> getAlertPage(ReviewAlertPageReqVO reqVO) {
        return alertMapper.selectPage(reqVO);
    }

    @Override
    public List<AmazonReviewAlertDO> getUnacknowledgedAlerts(Long shopId) {
        return alertMapper.selectUnacknowledged(shopId);
    }

    @Override
    public void acknowledgeAlert(Long alertId) {
        AmazonReviewAlertDO alert = alertMapper.selectById(alertId);
        if (alert != null) {
            alert.setAcknowledged(true);
            alertMapper.updateById(alert);
        }
    }
}
