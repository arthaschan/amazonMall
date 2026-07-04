package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import com.yudao.module.amazon.common.core.SpApiClient.SpApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Listing 同步服务实现。
 * <p>通过 SP-API CatalogItemsApi 拉取商品 Listing 并持久化到本地数据库。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class ListingSyncServiceImpl implements ListingSyncService {

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonProductMapper amazonProductMapper;

    @Resource
    private AmazonShopService amazonShopService;

    private static final String CATALOG_PATH = "/catalog/2022-04-01/items";

    @Override
    public void syncListings(Long shopId, String marketplaceId) {
        log.info("[ListingSync] 开始同步 Listing shopId={}, marketplaceId={}", shopId, marketplaceId);

        // 1. 加载店铺信息
        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            log.error("[ListingSync] 店铺不存在 shopId={}", shopId);
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }

        // 2. 解析 AWS Region
        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(marketplaceId);
        if (marketplace == null) {
            log.error("[ListingSync] 不支持的 marketplaceId={}", marketplaceId);
            throw new IllegalArgumentException("Unsupported marketplaceId: " + marketplaceId);
        }
        String awsRegion = marketplace.getAwsRegion();
        String sellerId = shop.getSellerId();

        // 3. 构建初始查询参数
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("marketplaceIds", marketplaceId);
        queryParams.put("identifiersType", "ASIN");
        queryParams.put("includedData", "summaries,images,salesRankings,attributes");

        int totalSynced = 0;
        int pageCount = 0;
        String nextToken = null;

        // 4. 分页拉取 Catalog Items
        do {
            if (nextToken != null) {
                queryParams.put("pageToken", nextToken);
            }

            SpApiResponse response;
            try {
                response = spApiClient.getFullResponse(sellerId, awsRegion, CATALOG_PATH, queryParams);
            } catch (Exception e) {
                log.error("[ListingSync] SP-API 调用失败 shopId={}, page={}", shopId, pageCount, e);
                throw new RuntimeException("SP-API catalog call failed: " + e.getMessage(), e);
            }

            if (!response.isSuccessful() || response.jsonBody() == null) {
                log.error("[ListingSync] SP-API 返回错误 statusCode={}, error={}",
                        response.statusCode(), response.getErrorMessage());
                throw new RuntimeException("SP-API catalog call returned error: " + response.getErrorMessage());
            }

            JsonNode root = response.jsonBody();
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                log.warn("[ListingSync] 响应中缺少 items 数组 shopId={}, page={}", shopId, pageCount);
                break;
            }

            // 5. 遍历每个 item，映射并持久化
            for (JsonNode item : items) {
                try {
                    AmazonProductDO product = mapToProductDO(item, shop, marketplaceId);
                    amazonProductMapper.insertOrUpdate(product);
                    totalSynced++;
                } catch (Exception e) {
                    String asin = item.path("asin").asText("UNKNOWN");
                    log.warn("[ListingSync] 处理 item 失败 asin={}", asin, e);
                }
            }

            pageCount++;
            log.info("[ListingSync] 第 {} 页处理完毕，本页 {} 条，累计 {} 条",
                    pageCount, items.size(), totalSynced);

            // 6. 解析分页 token
            nextToken = null;
            JsonNode pagination = root.path("pagination");
            if (!pagination.isMissingNode() && pagination.hasNonNull("nextToken")) {
                nextToken = pagination.get("nextToken").asText();
            }

        } while (nextToken != null);

        log.info("[ListingSync] 同步完成 shopId={}, marketplaceId={}, 共 {} 条，分 {} 页",
                shopId, marketplaceId, totalSynced, pageCount);
    }

    /**
     * 将 SP-API Catalog Item JSON 映射为 AmazonProductDO。
     */
    private AmazonProductDO mapToProductDO(JsonNode item, AmazonShopDO shop, String marketplaceId) {
        AmazonProductDO product = new AmazonProductDO();
        product.setShopId(shop.getId());
        product.setTenantId(shop.getTenantId());
        product.setMarketplaceId(marketplaceId);

        // ASIN
        product.setAsin(item.path("asin").asText(null));

        // summaries[0] -> title, price, listingStatus
        JsonNode summaries = item.path("summaries");
        if (summaries.isArray() && !summaries.isEmpty()) {
            JsonNode firstSummary = summaries.get(0);
            product.setTitle(firstSummary.path("itemName").asText(null));
            product.setListingStatus(firstSummary.path("status").asText(null));

            JsonNode price = firstSummary.path("price");
            if (!price.isMissingNode()) {
                product.setPrice(new BigDecimal(price.path("amount").asText("0")));
                product.setCurrency(price.path("currency").asText(null));
            }
        }

        // attributes -> brand
        JsonNode attributes = item.path("attributes");
        if (!attributes.isMissingNode()) {
            JsonNode brandNode = attributes.path("brand");
            if (brandNode.isArray() && !brandNode.isEmpty()) {
                product.setBrand(brandNode.get(0).asText(null));
            } else if (brandNode.isTextual()) {
                product.setBrand(brandNode.asText(null));
            }
        }

        // images[0].link -> mainImageUrl
        JsonNode images = item.path("images");
        if (images.isArray() && !images.isEmpty()) {
            product.setMainImageUrl(images.get(0).path("link").asText(null));
        }

        // salesRankings[0].rank -> bsrRank
        JsonNode salesRankings = item.path("salesRankings");
        if (salesRankings.isArray() && !salesRankings.isEmpty()) {
            JsonNode rankNode = salesRankings.get(0).path("rank");
            if (!rankNode.isMissingNode()) {
                product.setBsrRank(rankNode.asInt(0));
            }
        }

        return product;
    }
}
