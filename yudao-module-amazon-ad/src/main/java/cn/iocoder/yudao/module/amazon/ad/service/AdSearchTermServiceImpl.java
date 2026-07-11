package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.SearchTermPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdSearchTermDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdSearchTermMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 广告搜索词服务实现。
 * <p>提供搜索词分页查询和 AI 智能标签功能。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AdSearchTermServiceImpl implements AdSearchTermService {

    @Resource
    private AmazonAdSearchTermMapper searchTermMapper;

    @Override
    public PageResult<AmazonAdSearchTermDO> getSearchTermPage(SearchTermPageReqVO reqVO) {
        return searchTermMapper.selectPage(reqVO);
    }

    @Override
    public void tagSearchTerms(Long shopId, Long campaignId) {
        // 1. 构建查询条件
        LambdaQueryWrapperX<AmazonAdSearchTermDO> wrapper = new LambdaQueryWrapperX<AmazonAdSearchTermDO>()
                .eq(AmazonAdSearchTermDO::getShopId, shopId)
                .eqIfPresent(AmazonAdSearchTermDO::getCampaignId, campaignId)
                .ge(AmazonAdSearchTermDO::getReportDate, LocalDate.now().minusDays(30));

        // 2. 查询搜索词列表
        List<AmazonAdSearchTermDO> searchTerms = searchTermMapper.selectList(wrapper);
        if (searchTerms == null || searchTerms.isEmpty()) {
            log.info("[tagSearchTerms] shopId={}, campaignId={}, 无搜索词数据", shopId, campaignId);
            return;
        }

        // 3. 逐条分类打标
        int opportunity = 0;
        int waste = 0;
        int negative = 0;
        int keep = 0;

        for (AmazonAdSearchTermDO term : searchTerms) {
            String tag = classifyTerm(term);
            term.setAiTag(tag);
            searchTermMapper.updateById(term);

            if ("OPPORTUNITY".equals(tag)) {
                opportunity++;
            } else if ("NEGATIVE".equals(tag)) {
                negative++;
            } else if ("WASTE".equals(tag)) {
                waste++;
            } else {
                keep++;
            }
        }

        log.info("搜索词标签完成: OPPORTUNITY={}, WASTE={}, NEGATIVE={}, KEEP={}",
                opportunity, waste, negative, keep);
    }

    /**
     * 对单个搜索词进行分类。
     *
     * @param term 搜索词 DO
     * @return 标签: OPPORTUNITY / NEGATIVE / WASTE / KEEP
     */
    private String classifyTerm(AmazonAdSearchTermDO term) {
        Integer orders = term.getOrders() != null ? term.getOrders() : 0;
        BigDecimal sales = term.getSales() != null ? term.getSales() : BigDecimal.ZERO;
        BigDecimal cost = term.getCost();
        Long clicks = term.getClicks() != null ? term.getClicks() : 0L;

        // OPPORTUNITY: orders > 0 AND sales > 0 AND cost > 0 AND (sales/cost >= 2.0)
        if (orders > 0 && sales.compareTo(BigDecimal.ZERO) > 0
                && cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = sales.divide(cost, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("2.0")) >= 0) {
                return "OPPORTUNITY";
            }
        }

        // NEGATIVE: clicks >= 20 AND orders == 0 AND cost > 10
        if (clicks >= 20 && orders == 0
                && cost != null && cost.compareTo(new BigDecimal("10")) > 0) {
            return "NEGATIVE";
        }

        // WASTE: clicks >= 10 AND orders == 0 AND cost > 5
        if (clicks >= 10 && orders == 0
                && cost != null && cost.compareTo(new BigDecimal("5")) > 0) {
            return "WASTE";
        }

        return "KEEP";
    }
}
