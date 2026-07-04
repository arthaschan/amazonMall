package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;

/**
 * 库存同步服务实现。
 * <p>通过 SP-API InventoryApi 拉取 FBA 库存并持久化到本地数据库。</p>
 *
 * <p>流程：
 * <ol>
 *   <li>根据 shopId 加载店铺信息，获取 sellerId 并解析 AWS Region</li>
 *   <li>调用 SP-API {@code GET /fba/inventory/v1/summaries} 分页拉取 FBA 库存汇总</li>
 *   <li>映射字段到 AmazonInventoryDO 并批量持久化</li>
 *   <li>先清除当天已有快照数据，再插入新数据以保证幂等性</li>
 * </ol>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class InventorySyncServiceImpl implements InventorySyncService {

    /** SP-API FBA 库存汇总接口路径 */
    private static final String INVENTORY_SUMMARIES_PATH = "/fba/inventory/v1/summaries";

    /** NA 区域 Marketplace ID 集合 */
    private static final Set<String> NA_MARKETPLACES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "ATVPDKIKX0DER",   // US
                    "A2EUQ1WTGCTBG2",  // CA
                    "A1AM78C64UM0Y8",  // MX
                    "A2Q3Y263D00KWC"   // BR
            )));

    /** EU 区域 Marketplace ID 集合 */
    private static final Set<String> EU_MARKETPLACES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "A1F83G8C2ARO7P",  // UK
                    "A1PA6795UKMFR9",  // DE
                    "A13V1IB3VIYZZH",  // FR
                    "APJ6JRA9NG5V4",   // IT
                    "A1RKKUPIHCS9HS",  // ES
                    "A1805IZSGTT6HS",  // NL
                    "A2NODRKZP88ZB9",  // SE
                    "A1C3SOZRARQ6R3",  // PL
                    "AMEN7PMS3EDWL",   // BE
                    "A33AVAJ2PDY3EV"   // TR
            )));

    /** FE 区域 Marketplace ID 集合 */
    private static final Set<String> FE_MARKETPLACES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "A1VC38T7YXB528",  // JP
                    "A39IBJ37TR1ESG",  // AU
                    "A21TJRUUN4KGV",   // IN
                    "A19VAU5U5O7RUS"   // SG
            )));

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonInventoryMapper inventoryMapper;

    @Resource
    private AmazonShopService shopService;

    @Override
    public void syncInventory(Long shopId, String marketplaceId) {
        log.info("[InventorySync] 开始同步库存 shopId={}, marketplaceId={}", shopId, marketplaceId);

        // 1. 加载店铺信息
        AmazonShopDO shop = shopService.getShopById(shopId);
        if (shop == null) {
            log.error("[InventorySync] 店铺不存在 shopId={}", shopId);
            return;
        }
        String sellerId = shop.getSellerId();
        String awsRegion = resolveAwsRegion(marketplaceId);
        log.info("[InventorySync] 店铺信息加载成功 sellerId={}, awsRegion={}", sellerId, awsRegion);

        LocalDate today = LocalDate.now();
        int totalItems = 0;
        String nextToken = null;

        // 2. 清除当天该店铺的已有库存快照（保证幂等性）
        int deleted = inventoryMapper.delete(new LambdaQueryWrapper<AmazonInventoryDO>()
                .eq(AmazonInventoryDO::getShopId, shopId)
                .eq(AmazonInventoryDO::getSnapshotDate, today));
        if (deleted > 0) {
            log.info("[InventorySync] 已清除当天旧快照 shopId={}, snapshotDate={}, 删除数量={}",
                    shopId, today, deleted);
        }

        // 3. 分页拉取库存汇总
        List<AmazonInventoryDO> batchBuffer = new ArrayList<>();

        do {
            Map<String, String> queryParams = buildInventoryQueryParams(marketplaceId, nextToken);

            SpApiClient.SpApiResponse response;
            try {
                response = spApiClient.getFullResponse(sellerId, awsRegion, INVENTORY_SUMMARIES_PATH, queryParams);
            } catch (Exception e) {
                log.error("[InventorySync] SP-API 库存查询失败 shopId={}, marketplaceId={}", shopId, marketplaceId, e);
                break;
            }

            if (!response.isSuccessful() || response.jsonBody() == null) {
                log.error("[InventorySync] SP-API 库存查询返回错误 statusCode={}, error={}",
                        response.statusCode(), response.getErrorMessage());
                break;
            }

            JsonNode jsonBody = response.jsonBody();

            // 解析分页 Token（在 pagination 节点下）
            JsonNode paginationNode = jsonBody.get("pagination");
            if (paginationNode != null) {
                JsonNode nextTokenNode = paginationNode.get("nextToken");
                nextToken = (nextTokenNode != null && !nextTokenNode.isNull()) ? nextTokenNode.asText() : null;
            } else {
                nextToken = null;
            }

            // 解析库存汇总列表
            JsonNode payload = jsonBody.get("payload");
            if (payload == null) {
                log.warn("[InventorySync] 响应中缺少 payload 节点, requestId={}", response.requestId());
                break;
            }

            JsonNode summariesNode = payload.get("inventorySummaries");
            if (summariesNode == null || !summariesNode.isArray() || summariesNode.isEmpty()) {
                log.info("[InventorySync] 当前页无库存数据, 结束同步");
                break;
            }

            // 4. 遍历处理每条库存记录
            for (JsonNode summaryNode : summariesNode) {
                try {
                    AmazonInventoryDO inventoryDO = mapInventoryToDO(summaryNode, shop, today);
                    if (inventoryDO != null) {
                        batchBuffer.add(inventoryDO);
                        totalItems++;
                    }
                } catch (Exception e) {
                    String asin = summaryNode.has("asin") ? summaryNode.get("asin").asText() : "UNKNOWN";
                    log.error("[InventorySync] 处理库存记录失败 asin={}", asin, e);
                }
            }

            log.info("[InventorySync] 当前页处理完成, 本页记录数={}, 累计={}", summariesNode.size(), totalItems);

            // 批量入库（每 500 条刷一次，避免内存溢出）
            if (batchBuffer.size() >= 500) {
                flushBatch(batchBuffer);
            }

        } while (nextToken != null && !nextToken.isEmpty());

        // 刷入剩余数据
        if (!batchBuffer.isEmpty()) {
            flushBatch(batchBuffer);
        }

        log.info("[InventorySync] 库存同步完成 shopId={}, marketplaceId={}, 共同步记录数={}",
                shopId, marketplaceId, totalItems);
    }

    /**
     * 将 SP-API 库存汇总 JSON 映射为 AmazonInventoryDO。
     */
    private AmazonInventoryDO mapInventoryToDO(JsonNode summaryNode, AmazonShopDO shop, LocalDate snapshotDate) {
        String asin = getTextValue(summaryNode, "asin");
        if (asin == null || asin.isEmpty()) {
            log.warn("[InventorySync] 库存记录缺少 asin, 跳过");
            return null;
        }

        AmazonInventoryDO inventoryDO = new AmazonInventoryDO();
        inventoryDO.setAsin(asin);
        inventoryDO.setSku(getTextValue(summaryNode, "sellerSku"));
        inventoryDO.setFulfillmentCenter(getTextValue(summaryNode, "fulfillmentCenterId"));
        inventoryDO.setShopId(shop.getId());
        inventoryDO.setTenantId(shop.getTenantId());
        inventoryDO.setSnapshotDate(snapshotDate);

        // 解析库存明细
        JsonNode detailsNode = summaryNode.get("inventoryDetails");
        if (detailsNode != null) {
            inventoryDO.setAvailableQty(getIntValue(detailsNode, "availableQuantity"));
            inventoryDO.setReservedQty(getIntValue(detailsNode, "reservedQuantity"));

            // 入库中数量 = inboundWorking + inboundShipped + inboundReceiving
            int inboundWorking = getIntValue(detailsNode, "inboundWorkingQuantity");
            int inboundShipped = getIntValue(detailsNode, "inboundShippedQuantity");
            int inboundReceiving = getIntValue(detailsNode, "inboundReceivingQuantity");
            inventoryDO.setInboundQty(inboundWorking + inboundShipped + inboundReceiving);

            inventoryDO.setUnfulfillableQty(getIntValue(detailsNode, "unfulfillableQuantity"));
        }

        return inventoryDO;
    }

    /**
     * 批量刷入库存数据到数据库。
     */
    private void flushBatch(List<AmazonInventoryDO> buffer) {
        try {
            inventoryMapper.insertBatch(buffer);
            log.debug("[InventorySync] 批量插入库存记录数={}", buffer.size());
        } catch (Exception e) {
            log.error("[InventorySync] 批量插入库存数据失败, size={}", buffer.size(), e);
            // 降级为逐条插入，尽量保留数据
            for (AmazonInventoryDO item : buffer) {
                try {
                    inventoryMapper.insert(item);
                } catch (Exception ex) {
                    log.error("[InventorySync] 单条库存插入失败 asin={}, sku={}",
                            item.getAsin(), item.getSku(), ex);
                }
            }
        } finally {
            buffer.clear();
        }
    }

    /**
     * 构建库存查询参数。
     */
    private Map<String, String> buildInventoryQueryParams(String marketplaceId, String nextToken) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("marketplaceIds", marketplaceId);
        params.put("details", "true");
        if (nextToken != null && !nextToken.isEmpty()) {
            params.put("nextToken", nextToken);
        }
        return params;
    }

    /**
     * 安全地从 JsonNode 中获取文本值。
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        return null;
    }

    /**
     * 安全地从 JsonNode 中获取整数值，缺失或 null 时返回 0。
     */
    private int getIntValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asInt(0);
        }
        return 0;
    }

    /**
     * 根据 Marketplace ID 解析对应的 AWS Region。
     *
     * <p>映射规则：
     * <ul>
     *   <li>NA (US, CA, MX, BR) -> us-east-1</li>
     *   <li>EU (UK, DE, FR, IT, ES, NL, SE, PL, BE, TR) -> eu-west-1</li>
     *   <li>FE (JP, AU, IN, SG) -> us-west-2</li>
     * </ul>
     *
     * @param marketplaceId Amazon Marketplace ID
     * @return AWS Region 字符串
     */
    private String resolveAwsRegion(String marketplaceId) {
        if (marketplaceId == null) {
            log.warn("[InventorySync] marketplaceId 为空, 默认使用 us-east-1");
            return "us-east-1";
        }
        if (NA_MARKETPLACES.contains(marketplaceId)) {
            return "us-east-1";
        }
        if (EU_MARKETPLACES.contains(marketplaceId)) {
            return "eu-west-1";
        }
        if (FE_MARKETPLACES.contains(marketplaceId)) {
            return "us-west-2";
        }
        log.warn("[InventorySync] 未识别的 marketplaceId={}, 默认使用 us-east-1", marketplaceId);
        return "us-east-1";
    }
}
