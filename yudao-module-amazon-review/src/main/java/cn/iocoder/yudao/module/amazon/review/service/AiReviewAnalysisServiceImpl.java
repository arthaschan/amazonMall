package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiReviewAnalysisServiceImpl implements AiReviewAnalysisService {

    @Resource
    private AmazonReviewMapper reviewMapper;

    @Override
    public void analyzeReview(Long reviewId) {
        AmazonReviewDO review = reviewMapper.selectById(reviewId);
        if (review == null) return;
        // TODO: 调用 AI 模型进行情感分析和主题提取
        review.setAiSentiment(review.getRating() >= 4 ? "POSITIVE" :
                review.getRating() == 3 ? "NEUTRAL" : "NEGATIVE");
        reviewMapper.updateById(review);
    }

    @Override
    public void analyzeByAsin(Long shopId, String asin) {
        List<AmazonReviewDO> reviews = reviewMapper.selectByAsin(shopId, asin);
        for (AmazonReviewDO review : reviews) {
            // TODO: 批量 AI 分析
            analyzeReview(review.getId());
        }
    }

    @Override
    public Map<String, Object> getAggregateAnalysis(Long shopId, String asin) {
        List<AmazonReviewDO> reviews = reviewMapper.selectByAsin(shopId, asin);
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();

        // 情感分布
        Map<String, Long> sentimentCount = reviews.stream()
                .filter(r -> r.getAiSentiment() != null)
                .collect(Collectors.groupingBy(AmazonReviewDO::getAiSentiment, Collectors.counting()));
        result.put("sentimentDistribution", sentimentCount);

        // 高频主题
        Map<String, Long> allTopics = reviews.stream()
                .filter(r -> r.getAiTopics() != null)
                .flatMap(r -> r.getAiTopics().stream())
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        result.put("topTopics", allTopics.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10).collect(Collectors.toList()));

        // 痛点汇总
        List<String> allPainPoints = reviews.stream()
                .filter(r -> r.getAiPainPoints() != null)
                .flatMap(r -> r.getAiPainPoints().stream())
                .distinct().collect(Collectors.toList());
        result.put("painPoints", allPainPoints);

        // 卖点汇总
        List<String> allSellingPoints = reviews.stream()
                .filter(r -> r.getAiSellingPoints() != null)
                .flatMap(r -> r.getAiSellingPoints().stream())
                .distinct().collect(Collectors.toList());
        result.put("sellingPoints", allSellingPoints);

        return result;
    }
}
