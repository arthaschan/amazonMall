package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;

import java.util.List;
import java.util.Map;

/**
 * AI 评论分析 Service。
 * <p>对评论进行情感分析、主题提取、痛点/卖点挖掘。</p>
 *
 * @author AmazonOps AI
 */
public interface AiReviewAnalysisService {

    /** 分析单条评论 */
    void analyzeReview(Long reviewId);

    /** 批量分析指定 ASIN 的所有评论 */
    void analyzeByAsin(Long shopId, String asin);

    /**
     * 获取 ASIN 级别的评论聚合分析。
     *
     * @return key=分析维度, value=结果
     */
    Map<String, Object> getAggregateAnalysis(Long shopId, String asin);
}
