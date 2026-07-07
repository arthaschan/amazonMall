package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.CampaignPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdCampaignMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 广告活动同步与管理服务实现。
 *
 * <p>通过 SP-API 拉取 SP (Sponsored Products) / SB (Sponsored Brands) / SD (Sponsored Display)
 * 三种广告活动数据，映射为 {@link AmazonAdCampaignDO} 并持久化。</p>
 *
 * <p>注意：Amazon Advertising API 需要独立的 OAuth2 授权流程，
 * 包括 profileId 和 Amazon-Advertising-API-ClientId 请求头。
 * 当前实现使用 SpApiClient 基础设施，需要 Advertising API 授权完成后才能实际调用。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AdCampaignServiceImpl implements AdCampaignService {

    /** SP-API 广告活动路径 — Sponsored Products */
    private static final String SP_CAMPAIGNS_PATH = "/v2/sp/campaigns";
    /** SP-API 广告活动路径 — Sponsored Brands */
    private static final String SB_CAMPAIGNS_PATH = "/v2/sb/campaigns";
    /** SP-API 广告活动路径 — Sponsored Display */
    private static final String SD_CAMPAIGNS_PATH = "/v2/sd/campaigns";

    @Resource
    private AmazonAdCampaignMapper campaignMapper;

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonShopService amazonShopService;

    @Override
    public AmazonAdCampaignDO getCampaign(Long id) {
        return campaignMapper.selectById(id);
    }

    @Override
    public PageResult<AmazonAdCampaignDO> getCampaignPage(CampaignPageReqVO reqVO) {
        return campaignMapper.selectPage(reqVO);
    }

    @Override
    public void syncCampaigns(Long shopId) {
        log.info("[AdCampaign] 开始同步广告活动 shopId={}", shopId);

        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            log.error("[AdCampaign] 店铺不存在 shopId={}", shopId);
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }

        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(shop.getMarketplaceId());
        if (marketplace == null) {
            log.error("[AdCampaign] 不支持的 marketplaceId={}", shop.getMarketplaceId());
            throw new IllegalArgumentException("Unsupported marketplaceId: " + shop.getMarketplaceId());
        }

        String sellerId = shop.getSellerId();
        String awsRegion = marketplace.getAwsRegion();

        int totalSynced = 0;

        // 同步三种广告类型的活动
        totalSynced += syncCampaignType(sellerId, awsRegion, shopId, shop.getTenantId(),
                SP_CAMPAIGNS_PATH, "SP", "sponsoredProducts");
        totalSynced += syncCampaignType(sellerId, awsRegion, shopId, shop.getTenantId(),
                SB_CAMPAIGNS_PATH, "SB", "sponsoredBrands");
        totalSynced += syncCampaignType(sellerId, awsRegion, shopId, shop.getTenantId(),
                SD_CAMPAIGNS_PATH, "SD", "sponsoredDisplay");

        log.info("[AdCampaign] 广告活动同步完成 shopId={}, totalSynced={}", shopId, totalSynced);
    }

    /**
     * 同步指定类型的广告活动。
     *
     * @param sellerId     卖家 ID
     * @param awsRegion    AWS 区域
     * @param shopId       店铺 ID
     * @param tenantId     租户 ID
     * @param path         SP-API 路径
     * @param campaignType 广告类型代码（SP/SB/SD）
     * @param typeName     广告类型名称（日志用）
     * @return 同步的活动数量
     */
    private int syncCampaignType(String sellerId, String awsRegion, Long shopId, Long tenantId,
                                  String path, String campaignType, String typeName) {
        int synced = 0;

        try {
            // Advertising API 返回 JSON 数组（非标准 SP-API payload 包装）
            JsonNode response = spApiClient.get(sellerId, awsRegion, path, null);
            if (response == null) {
                log.warn("[AdCampaign] {} 活动查询返回空响应", typeName);
                return 0;
            }

            // 响应可能是数组或包含 campaigns 字段的对象
            JsonNode campaignsArray;
            if (response.isArray()) {
                campaignsArray = response;
            } else {
                campaignsArray = response.get("campaigns");
                if (campaignsArray == null) {
                    campaignsArray = response.get("sponsoredProductsCampaigns");
                }
                if (campaignsArray == null) {
                    log.warn("[AdCampaign] {} 响应中未找到活动数组", typeName);
                    return 0;
                }
            }

            for (JsonNode campaignNode : campaignsArray) {
                try {
                    AmazonAdCampaignDO campaign = mapCampaignToDO(campaignNode, shopId, tenantId, campaignType);
                    if (campaign != null) {
                        persistCampaign(campaign);
                        synced++;
                    }
                } catch (Exception e) {
                    log.warn("[AdCampaign] 解析{}活动失败: {}", typeName, e.getMessage());
                }
            }

            log.info("[AdCampaign] {} 活动同步完成, synced={}", typeName, synced);

        } catch (Exception e) {
            // Advertising API 需要独立 OAuth2 授权，未授权时优雅降级
            log.warn("[AdCampaign] {} 活动同步失败 (可能需要 Advertising API 授权): {}",
                    typeName, e.getMessage());
        }

        return synced;
    }

    /**
     * 将广告活动 JSON 映射为 AmazonAdCampaignDO。
     */
    private AmazonAdCampaignDO mapCampaignToDO(JsonNode node, Long shopId, Long tenantId,
                                                 String campaignType) {
        AmazonAdCampaignDO campaign = new AmazonAdCampaignDO();
        campaign.setShopId(shopId);
        campaign.setTenantId(tenantId);
        campaign.setCampaignType(campaignType);

        // 广告活动 ID（字段名因广告类型而异）
        Long campaignId = getLongValue(node, "campaignId");
        if (campaignId == null) {
            campaignId = getLongValue(node, "adGroupId");
        }
        campaign.setCampaignId(campaignId);

        // 活动名称
        campaign.setCampaignName(getTextValue(node, "name"));
        if (campaign.getCampaignName() == null) {
            campaign.setCampaignName(getTextValue(node, "campaignName"));
        }

        // 投放类型
        campaign.setTargetingType(getTextValue(node, "targetingType"));

        // 每日预算（单位: 分 → 元 或直接为元）
        String budgetStr = getTextValue(node, "dailyBudget");
        if (budgetStr != null) {
            try {
                campaign.setDailyBudget(new BigDecimal(budgetStr));
            } catch (NumberFormatException e) {
                log.debug("[AdCampaign] 无法解析预算值: {}", budgetStr);
            }
        }

        // 状态
        campaign.setStatus(getTextValue(node, "state"));
        if (campaign.getStatus() == null) {
            campaign.setStatus(getTextValue(node, "status"));
        }

        // 日期
        String startDateStr = getTextValue(node, "startDate");
        if (startDateStr != null) {
            campaign.setStartDate(parseDateSafe(startDateStr));
        }
        String endDateStr = getTextValue(node, "endDate");
        if (endDateStr != null) {
            campaign.setEndDate(parseDateSafe(endDateStr));
        }

        return campaign;
    }

    /**
     * 持久化广告活动：根据 shopId + campaignId 查询已有记录，存在则更新，不存在则新增。
     */
    private void persistCampaign(AmazonAdCampaignDO campaign) {
        if (campaign.getCampaignId() == null) {
            log.warn("[AdCampaign] 活动缺少 campaignId, 跳过");
            return;
        }

        AmazonAdCampaignDO existing = campaignMapper.selectOne(
                new LambdaQueryWrapper<AmazonAdCampaignDO>()
                        .eq(AmazonAdCampaignDO::getShopId, campaign.getShopId())
                        .eq(AmazonAdCampaignDO::getCampaignId, campaign.getCampaignId())
                        .last("LIMIT 1"));

        if (existing != null) {
            campaign.setId(existing.getId());
            campaignMapper.updateById(campaign);
        } else {
            campaignMapper.insert(campaign);
        }
    }

    private static String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        return null;
    }

    private static Long getLongValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull() && fieldNode.isNumber()) {
            return fieldNode.asLong();
        }
        return null;
    }

    private static LocalDate parseDateSafe(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
