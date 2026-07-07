package cn.iocoder.yudao.module.amazon.listing.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * Listing 同步 Job
 * <p>
 * 从 Amazon SP-API Catalog Items API 拉取商品目录信息，包括标题、五点描述、
 * 品牌、价格、BSR 排名、评分、评论数等，同步到本地 amazon_product 表。
 * <p>
 * 使用 Catalog Items 2020-12-01 API 搜索店铺下所有商品：
 * {@code GET /catalog/2020-12-01/items?keywords=*&marketplaceId=xxx}
 * <p>
 * 处理器名称: amazonListingSyncJob
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("amazonListingSyncJob")
public class AmazonListingSyncJob extends AbstractAmazonSyncJob {

    /** SP-API Catalog Items 2020-12-01 搜索路径 */
    private static final String CATALOG_SEARCH_PATH = "/catalog/2020-12-01/items";

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonProductMapper productMapper;

    @Override
    protected String getJobName() {
        return "amazonListingSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) throws Exception {
        log.info("[ListingSync] 开始 Listing 同步 | shopId={}, marketplace={}",
                shop.getId(), shop.getMarketplaceId());

        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(shop.getMarketplaceId());
        if (marketplace == null) {
            log.error("[ListingSync] 不支持的 marketplaceId={}", shop.getMarketplaceId());
            return;
        }

        String sellerId = shop.getSellerId();
        String awsRegion = marketplace.getAwsRegion();

        int totalSynced = 0;
        String nextToken = null;

        // 使用 keywords=* 搜索店铺下所有商品（Catalog Items 2020-12-01）
        do {
            Map<String, String> queryParams = buildCatalogQueryParams(shop.getMarketplaceId(), nextToken);

            SpApiClient.SpApiResponse response;
            try {
                response = spApiClient.getFullResponse(sellerId, awsRegion, CATALOG_SEARCH_PATH, queryParams);
            } catch (Exception e) {
                log.error("[ListingSync] SP-API Catalog Items 查询失败 shopId={}", shop.getId(), e);
                break;
            }

            if (!response.isSuccessful() || response.getJsonBody() == null) {
                log.error("[ListingSync] SP-API Catalog Items 返回错误 statusCode={}, error={}",
                        response.getStatusCode(), response.getErrorMessage());
                break;
            }

            JsonNode jsonBody = response.getJsonBody();

            // 解析分页 Token
            JsonNode paginationNode = jsonBody.get("Pagination");
            if (paginationNode != null) {
                JsonNode nextTokenNode = paginationNode.get("NextToken");
                nextToken = (nextTokenNode != null && !nextTokenNode.isNull())
                        ? nextTokenNode.asText() : null;
            } else {
                nextToken = null;
            }

            // 解析商品列表
            JsonNode itemsNode = jsonBody.get("Items");
            if (itemsNode == null || !itemsNode.isArray() || itemsNode.isEmpty()) {
                log.info("[ListingSync] 当前页无商品数据, 结束同步");
                break;
            }

            for (JsonNode itemNode : itemsNode) {
                try {
                    AmazonProductDO product = mapCatalogItemToDO(itemNode, shop);
                    if (product != null) {
                        persistProduct(product);
                        totalSynced++;
                    }
                } catch (Exception e) {
                    String asin = getTextValue(itemNode, "asin");
                    log.error("[ListingSync] 处理商品失败 asin={}", asin, e);
                }
            }

            log.info("[ListingSync] 当前页处理完成, 本页商品数={}, 累计={}",
                    itemsNode.size(), totalSynced);

        } while (nextToken != null && !nextToken.isEmpty());

        log.info("[ListingSync] Listing 同步完成 | shopId={}, totalSynced={}",
                shop.getId(), totalSynced);
    }

    /**
     * 将 Catalog Items API 返回的商品 JSON 映射为 AmazonProductDO。
     *
     * <p>Catalog Items 2020-12-01 响应结构示例：
     * <pre>
     * {
     *   "asin": "B08N5KWB9H",
     *   "summaries": [{ "itemName": "...", "brandName": "...", "itemClassification": "BASE_PRODUCT" }],
     *   "salesRanks": [{ "marketplaceId": "...", "classificationRanks": [{ "title": "...", "rank": 123 }] }],
     *   "productTypes": [{ "productType": "PRODUCT" }]
     * }
     * </pre>
     */
    private AmazonProductDO mapCatalogItemToDO(JsonNode itemNode, AmazonShopDO shop) {
        String asin = getTextValue(itemNode, "asin");
        if (asin == null || asin.trim().isEmpty()) {
            log.warn("[ListingSync] 商品缺少 asin, 跳过");
            return null;
        }

        AmazonProductDO product = new AmazonProductDO();
        product.setAsin(asin);
        product.setShopId(shop.getId());
        product.setTenantId(shop.getTenantId());
        product.setMarketplaceId(shop.getMarketplaceId());

        // summaries 数组包含商品基本信息
        JsonNode summariesNode = itemNode.get("summaries");
        if (summariesNode != null && summariesNode.isArray() && summariesNode.size() > 0) {
            JsonNode summary = summariesNode.get(0);
            product.setTitle(getTextValue(summary, "itemName"));
            product.setBrand(getTextValue(summary, "brandName"));

            // 主图 URL
            JsonNode mainImageNode = summary.get("mainImage");
            if (mainImageNode != null) {
                product.setMainImageUrl(getTextValue(mainImageNode, "link"));
            }

            // Listing 状态
            String itemClassification = getTextValue(summary, "itemClassification");
            if (itemClassification != null) {
                product.setListingStatus("ACTIVE"); // Catalog API 返回的商品默认为 ACTIVE
            }
        }

        // 销售排名（BSR）
        JsonNode salesRanksNode = itemNode.get("salesRanks");
        if (salesRanksNode != null && salesRanksNode.isArray()) {
            for (JsonNode rankEntry : salesRanksNode) {
                JsonNode classificationRanks = rankEntry.get("classificationRanks");
                if (classificationRanks != null && classificationRanks.isArray()
                        && classificationRanks.size() > 0) {
                    JsonNode firstRank = classificationRanks.get(0);
                    JsonNode rankNode = firstRank.get("rank");
                    if (rankNode != null && !rankNode.isNull()) {
                        product.setBsrRank(rankNode.asInt());
                        break; // 取第一个分类排名作为 BSR
                    }
                }
            }
        }

        // 评分和评论数（来自 reviews 节点，如果存在）
        JsonNode reviewsNode = itemNode.get("reviews");
        if (reviewsNode != null) {
            JsonNode ratingNode = reviewsNode.get("rating");
            if (ratingNode != null && !ratingNode.isNull()) {
                product.setRating(new BigDecimal(ratingNode.asText()));
            }
            JsonNode countNode = reviewsNode.get("count");
            if (countNode != null && !countNode.isNull()) {
                product.setReviewCount(countNode.asInt());
            }
        }

        // 价格和货币（来自 offers 节点，如果存在）
        JsonNode offersNode = itemNode.get("offers");
        if (offersNode != null && offersNode.isArray() && offersNode.size() > 0) {
            JsonNode firstOffer = offersNode.get(0);
            JsonNode priceNode = firstOffer.get("price");
            if (priceNode != null) {
                String amount = getTextValue(priceNode, "amount");
                if (amount != null) {
                    product.setPrice(new BigDecimal(amount));
                }
                product.setCurrency(getTextValue(priceNode, "currency"));
            }
        }

        return product;
    }

    /**
     * 持久化商品：根据 shopId + asin 查询已有记录，存在则更新，不存在则新增。
     */
    private void persistProduct(AmazonProductDO product) {
        AmazonProductDO existing = productMapper.selectOne(
                new LambdaQueryWrapper<AmazonProductDO>()
                        .eq(AmazonProductDO::getShopId, product.getShopId())
                        .eq(AmazonProductDO::getAsin, product.getAsin())
                        .last("LIMIT 1"));

        if (existing != null) {
            product.setId(existing.getId());
            productMapper.updateById(product);
        } else {
            productMapper.insert(product);
        }
    }

    /**
     * 构建 Catalog Items 搜索查询参数。
     */
    private Map<String, String> buildCatalogQueryParams(String marketplaceId, String nextToken) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("keywords", "*");
        params.put("marketplaceId", marketplaceId);
        params.put("includedData", "summaries,salesRanks,offers,reviews");
        params.put("pageSize", "20");
        if (nextToken != null && !nextToken.isEmpty()) {
            params.put("pageToken", nextToken);
        }
        return params;
    }

    private static String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        return null;
    }
}
