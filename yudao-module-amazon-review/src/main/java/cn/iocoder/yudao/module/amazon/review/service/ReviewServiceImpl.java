package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewPageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Resource
    private AmazonReviewMapper reviewMapper;

    @Override
    public PageResult<AmazonReviewDO> getReviewPage(ReviewPageReqVO reqVO) {
        return reviewMapper.selectPage(reqVO);
    }

    @Override
    public List<AmazonReviewDO> getByAsin(Long shopId, String asin) {
        return reviewMapper.selectByAsin(shopId, asin);
    }

    @Override
    public void syncReviews(Long shopId, String asin) {
        // TODO: 通过 Amazon Product Advertising API 或爬虫获取评论
    }
}
