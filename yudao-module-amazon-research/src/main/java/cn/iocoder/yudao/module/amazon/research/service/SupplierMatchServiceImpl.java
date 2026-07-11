package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonProductOpportunityDO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonSupplierMatchDO;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonProductOpportunityMapper;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonSupplierMatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 供应商匹配服务实现。
 * <p>根据产品机会从 1688/Alibaba 等供应平台搜索匹配供应商。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class SupplierMatchServiceImpl implements SupplierMatchService {

    /** 常见英文停用词，用于从商品标题中提取有效关键词 */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "is", "it", "this", "that", "are", "was", "be",
            "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "may", "might", "can", "not", "no", "so", "if", "as", "from", "up",
            "out", "about", "into", "than", "then", "new", "hot", "sale", "best",
            "top", "free", "shipping", "pack", "pcs", "piece", "pieces", "set",
            "lot", "size", "color", "black", "white", "red", "blue", "green",
            "large", "small", "medium", "mini", "big", "xl", "xxl", "inch"
    ));

    @Resource
    private AmazonSupplierMatchMapper supplierMatchMapper;

    @Resource
    private AmazonProductOpportunityMapper opportunityMapper;

    @Override
    public List<AmazonSupplierMatchDO> matchSuppliers(Long opportunityId) {
        AmazonProductOpportunityDO opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity == null) {
            log.warn("产品机会不存在, opportunityId: {}", opportunityId);
            return Collections.emptyList();
        }

        String title = opportunity.getTitle();
        if (title == null || title.isEmpty()) {
            log.warn("产品机会标题为空, opportunityId: {}", opportunityId);
            return supplierMatchMapper.selectByOpportunityId(opportunityId);
        }

        // 从标题中提取关键词: 按空格拆分, 过滤停用词和短词
        String[] words = title.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String word : words) {
            String lower = word.toLowerCase().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
            if (lower.length() >= 2 && !STOP_WORDS.contains(lower)) {
                keywords.add(lower);
            }
        }

        if (keywords.isEmpty()) {
            log.warn("从标题中未能提取有效关键词, title: {}", title);
            return supplierMatchMapper.selectByOpportunityId(opportunityId);
        }

        // 拼接关键词并构建 1688 搜索 URL
        try {
            String keywordStr = joinStrings(keywords, " ");
            String encodedKeywords = URLEncoder.encode(keywordStr, "UTF-8");
            String searchUrl = "https://s.1688.com/selloffer/offer_search.htm?keywords=" + encodedKeywords;
            log.info("供应商匹配框架已就绪, 搜索URL: {}", searchUrl);
        } catch (Exception e) {
            log.warn("构建供应商搜索URL异常, opportunityId: {}", opportunityId, e);
        }

        log.info("1688/Alibaba API 集成待配置, 当前返回数据库缓存结果");
        return supplierMatchMapper.selectByOpportunityId(opportunityId);
    }

    @Override
    public List<AmazonSupplierMatchDO> getByOpportunityId(Long opportunityId) {
        return supplierMatchMapper.selectByOpportunityId(opportunityId);
    }

    /**
     * 将字符串列表用分隔符拼接（JDK 8 兼容方式）。
     */
    private static String joinStrings(List<String> list, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
