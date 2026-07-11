package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 评论分析 Service 实现。
 * <p>基于文本规则对评论进行情感分析、主题提取、痛点/卖点挖掘。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AiReviewAnalysisServiceImpl implements AiReviewAnalysisService {

    @Resource
    private AmazonReviewMapper reviewMapper;

    /** 停用词集合 */
    private static final Set<String> STOP_WORDS = new HashSet<String>(Arrays.asList(
            "the", "is", "and", "or", "it", "this", "that", "was", "for", "with",
            "my", "very", "just", "but", "not", "are", "had", "has", "have", "been",
            "were", "they", "their", "them", "what", "which", "who", "when", "where",
            "how", "all", "each", "every", "both", "few", "more", "most", "other",
            "some", "such", "than", "too", "can", "will", "would", "could", "should",
            "did", "does", "do", "get", "got", "its", "also", "from", "out", "about",
            "into", "over", "after", "then", "one", "two", "any", "these", "those",
            "there", "here", "his", "her", "she", "his", "him", "our", "you", "your",
            "we", "me", "he", "so", "if", "no", "up", "on", "at", "to", "in", "of",
            "as", "by", "an", "be", "am", "was", "being", "have", "has", "had",
            "not", "but", "what", "all", "were", "when", "we", "there", "been", "only",
            "really", "much", "even", "still", "already", "quite", "rather", "pretty"
    ));

    /** 负面关键词 */
    private static final List<String> NEGATIVE_KEYWORDS = Arrays.asList(
            "broken", "poor", "cheap", "difficult", "bad", "terrible", "waste",
            "problem", "issue", "fail", "worst", "disappointed", "uncomfortable",
            "flimsy", "defective", "leaks", "noisy", "heavy", "slow",
            "too small", "too big", "not enough", "stopped working"
    );

    /** 正面关键词 */
    private static final List<String> POSITIVE_KEYWORDS = Arrays.asList(
            "love", "great", "perfect", "excellent", "amazing", "easy", "comfortable",
            "sturdy", "durable", "quality", "works well", "well made", "best",
            "fantastic", "impressive", "worth", "recommend", "lightweight", "fast",
            "convenient"
    );

    @Override
    public void analyzeReview(Long reviewId) {
        AmazonReviewDO review = reviewMapper.selectById(reviewId);
        if (review == null) {
            log.warn("评论不存在，跳过分析: reviewId={}", reviewId);
            return;
        }

        Integer rating = review.getRating();
        String title = review.getTitle() != null ? review.getTitle() : "";
        String body = review.getBody() != null ? review.getBody() : "";
        String combinedText = (title + " " + body).trim();

        // --- 情感分析 ---
        String sentiment = analyzeSentiment(review);
        review.setAiSentiment(sentiment);

        // --- 主题提取 ---
        List<String> topics = extractTopics(combinedText);
        review.setAiTopics(topics);

        // --- 痛点提取 (rating <= 3) ---
        if (rating != null && rating <= 3) {
            List<String> painPoints = extractKeySentences(body, NEGATIVE_KEYWORDS, 3);
            review.setAiPainPoints(painPoints);
        } else {
            review.setAiPainPoints(new ArrayList<String>());
        }

        // --- 卖点提取 (rating >= 4) ---
        if (rating != null && rating >= 4) {
            List<String> sellingPoints = extractKeySentences(body, POSITIVE_KEYWORDS, 3);
            review.setAiSellingPoints(sellingPoints);
        } else {
            review.setAiSellingPoints(new ArrayList<String>());
        }

        reviewMapper.updateById(review);
        log.info("评论分析完成: reviewId={}, sentiment={}, topics={}, painPoints={}, sellingPoints={}",
                reviewId, sentiment, topics, review.getAiPainPoints(), review.getAiSellingPoints());
    }

    @Override
    public void analyzeByAsin(Long shopId, String asin) {
        List<AmazonReviewDO> reviews = reviewMapper.selectByAsin(shopId, asin);
        log.info("开始批量分析 ASIN={} 的评论，共 {} 条", asin, reviews.size());
        int processed = 0;
        for (AmazonReviewDO review : reviews) {
            try {
                analyzeReview(review.getId());
                processed++;
                if (processed % 50 == 0) {
                    log.info("ASIN={} 分析进度: {}/{}", asin, processed, reviews.size());
                }
            } catch (Exception e) {
                log.error("分析评论失败: reviewId={}, error={}", review.getId(), e.getMessage(), e);
            }
        }
        log.info("ASIN={} 批量分析完成: 成功 {}/{}", asin, processed, reviews.size());
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

    // ========================= 私有分析方法 =========================

    /**
     * 多因子情感分析。
     * <ul>
     *   <li>rating 1-2 → NEGATIVE</li>
     *   <li>rating 4-5 → POSITIVE（verifiedPurchase 增加置信度）</li>
     *   <li>rating 3 → 根据正文负面关键词密度判定</li>
     * </ul>
     */
    private String analyzeSentiment(AmazonReviewDO review) {
        Integer rating = review.getRating();
        if (rating == null) {
            return "NEUTRAL";
        }

        if (rating <= 2) {
            return "NEGATIVE";
        }

        if (rating >= 4) {
            // verifiedPurchase + 高评分 → 更确定的正面
            if (Boolean.TRUE.equals(review.getVerifiedPurchase())) {
                return "POSITIVE";
            }
            return "POSITIVE";
        }

        // rating == 3: 通过文本分析决定
        String body = review.getBody() != null ? review.getBody().toLowerCase() : "";
        String title = review.getTitle() != null ? review.getTitle().toLowerCase() : "";
        String text = title + " " + body;

        int negativeHits = 0;
        for (String keyword : NEGATIVE_KEYWORDS) {
            if (text.contains(keyword)) {
                negativeHits++;
            }
        }

        int positiveHits = 0;
        for (String keyword : POSITIVE_KEYWORDS) {
            if (text.contains(keyword)) {
                positiveHits++;
            }
        }

        if (negativeHits > positiveHits) {
            return "NEGATIVE";
        } else if (positiveHits > negativeHits) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    /**
     * 从文本中提取主题：分词 → 去停用词 → 按词频取 Top 5。
     */
    private List<String> extractTopics(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<String>();
        }

        // 分词：转小写，只保留字母
        String[] rawTokens = text.toLowerCase().split("[^a-zA-Z]+");
        Map<String, Integer> freq = new HashMap<String, Integer>();
        for (String token : rawTokens) {
            if (token.length() < 3) {
                continue;
            }
            if (STOP_WORDS.contains(token)) {
                continue;
            }
            Integer count = freq.get(token);
            freq.put(token, count == null ? 1 : count + 1);
        }

        // 按词频降序排序，取前 5 个
        List<Map.Entry<String, Integer>> sorted = new ArrayList<Map.Entry<String, Integer>>(freq.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        List<String> topics = new ArrayList<String>();
        int limit = Math.min(5, sorted.size());
        for (int i = 0; i < limit; i++) {
            topics.add(sorted.get(i).getKey());
        }
        return topics;
    }

    /**
     * 从正文中提取包含指定关键词的句子。
     *
     * @param body     评论正文
     * @param keywords 关键词列表
     * @param maxCount 最多返回的句子数
     * @return 匹配的句子列表（每条最多 100 字符）
     */
    private List<String> extractKeySentences(String body, List<String> keywords, int maxCount) {
        if (body == null || body.isEmpty()) {
            return new ArrayList<String>();
        }

        // 按句号/感叹号/问号拆分句子
        String[] sentences = body.split("[.!?]+");
        List<String> results = new ArrayList<String>();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase();

            boolean matched = false;
            for (String keyword : keywords) {
                if (lower.contains(keyword)) {
                    matched = true;
                    break;
                }
            }

            if (matched) {
                // 截断到 100 字符
                String result = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
                // 去重：避免完全相同的句子
                if (!results.contains(result)) {
                    results.add(result);
                }
                if (results.size() >= maxCount) {
                    break;
                }
            }
        }
        return results;
    }
}
