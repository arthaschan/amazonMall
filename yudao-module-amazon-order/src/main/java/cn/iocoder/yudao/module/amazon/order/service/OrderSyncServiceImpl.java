package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderItemMapper;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单同步实现。
 * <p>通过 SP-API 拉取订单数据并持久化到本地数据库。</p>
 *
 * <p>流程：
 * <ol>
 *   <li>根据 shopId 加载店铺信息，获取 sellerId 并解析 AWS Region</li>
 *   <li>调用 SP-API {@code GET /orders/v0/orders} 分页拉取最近 30 天订单</li>
 *   <li>对每个订单调用 {@code GET /orders/v0/orders/{id}/orderItems} 拉取明细</li>
 *   <li>持久化订单和明细到本地数据库（存在则更新，不存在则新增）</li>
 * </ol>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class OrderSyncServiceImpl implements OrderSyncService {

    /** SP-API 订单接口路径 */
    private static final String ORDERS_PATH = "/orders/v0/orders";

    /** SP-API 订单明细接口路径模板 */
    private static final String ORDER_ITEMS_PATH_TEMPLATE = "/orders/v0/orders/%s/orderItems";

    /** 拉取订单的天数范围 */
    private static final int ORDER_LOOKBACK_DAYS = 30;

    /** ISO 8601 日期时间解析格式（兼容 Amazon 返回的多种格式） */
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

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
    private AmazonOrderMapper orderMapper;

    @Resource
    private AmazonOrderItemMapper orderItemMapper;

    @Resource
    private AmazonShopService shopService;

    @Override
    public void syncOrders(Long shopId, String marketplaceId) {
        log.info("[OrderSync] 开始同步订单 shopId={}, marketplaceId={}", shopId, marketplaceId);

        // 1. 加载店铺信息
        AmazonShopDO shop = shopService.getShopById(shopId);
        if (shop == null) {
            log.error("[OrderSync] 店铺不存在 shopId={}", shopId);
            return;
        }
        String sellerId = shop.getSellerId();
        String awsRegion = resolveAwsRegion(marketplaceId);
        log.info("[OrderSync] 店铺信息加载成功 sellerId={}, awsRegion={}", sellerId, awsRegion);

        // 2. 构建查询参数：最近 30 天创建的订单
        String createdAfter = LocalDateTime.now()
                .minusDays(ORDER_LOOKBACK_DAYS)
                .format(DateTimeFormatter.ISO_DATE_TIME);

        int totalOrders = 0;
        String nextToken = null;

        // 3. 分页拉取订单
        do {
            Map<String, String> queryParams = buildOrderQueryParams(marketplaceId, createdAfter, nextToken);

            SpApiClient.SpApiResponse response;
            try {
                response = spApiClient.getFullResponse(sellerId, awsRegion, ORDERS_PATH, queryParams);
            } catch (Exception e) {
                log.error("[OrderSync] SP-API 订单查询失败 shopId={}, marketplaceId={}", shopId, marketplaceId, e);
                break;
            }

            if (!response.isSuccessful() || response.getJsonBody() == null) {
                log.error("[OrderSync] SP-API 订单查询返回错误 statusCode={}, error={}",
                        response.getStatusCode(), response.getErrorMessage());
                break;
            }

            JsonNode jsonBody = response.getJsonBody();
            JsonNode payload = jsonBody.get("payload");
            if (payload == null) {
                log.warn("[OrderSync] 响应中缺少 payload 节点, requestId={}", response.getRequestId());
                break;
            }

            // 解析分页 Token
            JsonNode nextTokenNode = payload.get("NextToken");
            nextToken = (nextTokenNode != null && !nextTokenNode.isNull()) ? nextTokenNode.asText() : null;

            // 解析订单列表
            JsonNode ordersNode = payload.get("Orders");
            if (ordersNode == null || !ordersNode.isArray() || ordersNode.isEmpty()) {
                log.info("[OrderSync] 当前页无订单数据, 结束同步");
                break;
            }

            // 4. 遍历处理每个订单
            for (JsonNode orderNode : ordersNode) {
                try {
                    processOrder(orderNode, shop);
                    totalOrders++;
                } catch (Exception e) {
                    String orderId = orderNode.has("AmazonOrderId")
                            ? orderNode.get("AmazonOrderId").asText() : "UNKNOWN";
                    log.error("[OrderSync] 处理订单失败 amazonOrderId={}", orderId, e);
                }
            }

            log.info("[OrderSync] 当前页处理完成, 本页订单数={}, 累计={}", ordersNode.size(), totalOrders);

        } while (nextToken != null && !nextToken.isEmpty());

        log.info("[OrderSync] 订单同步完成 shopId={}, marketplaceId={}, 共同步订单数={}",
                shopId, marketplaceId, totalOrders);
    }

    /**
     * 处理单个订单：映射字段、持久化订单、拉取并持久化订单明细。
     */
    private void processOrder(JsonNode orderNode, AmazonShopDO shop) {
        String amazonOrderId = getTextValue(orderNode, "AmazonOrderId");
        if (amazonOrderId == null || amazonOrderId.isEmpty()) {
            log.warn("[OrderSync] 订单缺少 AmazonOrderId, 跳过");
            return;
        }

        // 映射订单字段到 DO
        AmazonOrderDO orderDO = mapOrderToDO(orderNode, amazonOrderId, shop);

        // 查询是否已存在
        AmazonOrderDO existingOrder = orderMapper.selectByAmazonOrderId(amazonOrderId);
        if (existingOrder != null) {
            orderDO.setId(existingOrder.getId());
            orderMapper.updateById(orderDO);
            log.debug("[OrderSync] 更新已有订单 amazonOrderId={}", amazonOrderId);
        } else {
            orderMapper.insert(orderDO);
            log.debug("[OrderSync] 新增订单 amazonOrderId={}", amazonOrderId);
        }

        // 拉取并持久化订单明细
        syncOrderItems(amazonOrderId, orderDO.getId(), shop);
    }

    /**
     * 将 SP-API 订单 JSON 映射为 AmazonOrderDO。
     */
    private AmazonOrderDO mapOrderToDO(JsonNode orderNode, String amazonOrderId, AmazonShopDO shop) {
        AmazonOrderDO orderDO = new AmazonOrderDO();
        orderDO.setAmazonOrderId(amazonOrderId);
        orderDO.setShopId(shop.getId());
        orderDO.setTenantId(shop.getTenantId());
        orderDO.setMarketplaceId(shop.getMarketplaceId());

        // 订单状态
        orderDO.setOrderStatus(getTextValue(orderNode, "OrderStatus"));

        // 订单总金额
        JsonNode orderTotalNode = orderNode.get("OrderTotal");
        if (orderTotalNode != null) {
            String amountStr = getTextValue(orderTotalNode, "Amount");
            if (amountStr != null) {
                orderDO.setOrderTotal(new BigDecimal(amountStr));
            }
            orderDO.setCurrency(getTextValue(orderTotalNode, "CurrencyCode"));
        }

        // 下单时间（ISO 8601）
        String purchaseDateStr = getTextValue(orderNode, "PurchaseDate");
        if (purchaseDateStr != null) {
            try {
                orderDO.setPurchaseDate(LocalDateTime.parse(purchaseDateStr, ISO_DATE_TIME));
            } catch (DateTimeParseException e) {
                log.warn("[OrderSync] 无法解析下单时间 purchaseDate={}", purchaseDateStr);
            }
        }

        // 配送渠道
        orderDO.setFulfillmentChannel(getTextValue(orderNode, "FulfillmentChannel"));

        // Prime 标识
        JsonNode isPrimeNode = orderNode.get("IsPrime");
        if (isPrimeNode != null && !isPrimeNode.isNull()) {
            orderDO.setIsPrime(isPrimeNode.asBoolean());
        }

        // 企业订单标识
        JsonNode isBusinessOrderNode = orderNode.get("IsBusinessOrder");
        if (isBusinessOrderNode != null && !isBusinessOrderNode.isNull()) {
            orderDO.setIsBusinessOrder(isBusinessOrderNode.asBoolean());
        }

        // 收货地址
        JsonNode shippingAddress = orderNode.get("ShippingAddress");
        if (shippingAddress != null) {
            orderDO.setShipCity(getTextValue(shippingAddress, "City"));
            orderDO.setShipState(getTextValue(shippingAddress, "StateOrRegion"));
            orderDO.setShipCountry(getTextValue(shippingAddress, "CountryCode"));
        }

        return orderDO;
    }

    /**
     * 同步订单明细：调用 SP-API 获取 orderItems 并持久化。
     * 先删除已有明细，再重新插入（保证幂等性）。
     */
    private void syncOrderItems(String amazonOrderId, Long orderId, AmazonShopDO shop) {
        String sellerId = shop.getSellerId();
        String awsRegion = resolveAwsRegion(shop.getMarketplaceId());
        String path = String.format(ORDER_ITEMS_PATH_TEMPLATE, amazonOrderId);

        List<AmazonOrderItemDO> items = new ArrayList<>();
        String nextToken = null;

        do {
            Map<String, String> queryParams = new LinkedHashMap<>();
            if (nextToken != null) {
                queryParams.put("NextToken", nextToken);
            }

            JsonNode response;
            try {
                response = spApiClient.get(sellerId, awsRegion, path, queryParams);
            } catch (Exception e) {
                log.error("[OrderSync] 拉取订单明细失败 amazonOrderId={}", amazonOrderId, e);
                return;
            }

            if (response == null) {
                break;
            }

            JsonNode payload = response.get("payload");
            if (payload == null) {
                break;
            }

            // 解析分页
            JsonNode nextTokenNode = payload.get("NextToken");
            nextToken = (nextTokenNode != null && !nextTokenNode.isNull()) ? nextTokenNode.asText() : null;

            // 解析明细列表
            JsonNode orderItemsNode = payload.get("OrderItems");
            if (orderItemsNode == null || !orderItemsNode.isArray()) {
                break;
            }

            for (JsonNode itemNode : orderItemsNode) {
                AmazonOrderItemDO itemDO = mapOrderItemToDO(itemNode, orderId);
                if (itemDO != null) {
                    items.add(itemDO);
                }
            }

        } while (nextToken != null && !nextToken.isEmpty());

        // 先删除旧明细，再批量插入新明细（保证幂等）
        if (orderId != null) {
            List<AmazonOrderItemDO> existingItems = orderItemMapper.selectByOrderId(orderId);
            if (!existingItems.isEmpty()) {
                orderItemMapper.deleteBatchIds(
                        existingItems.stream().map(AmazonOrderItemDO::getId).collect(Collectors.toList()));
            }
        }

        if (!items.isEmpty()) {
            orderItemMapper.insertBatch(items);
            log.debug("[OrderSync] 订单明细已保存 amazonOrderId={}, itemCount={}", amazonOrderId, items.size());
        }

        // 回写订单商品数量
        if (orderId != null) {
            AmazonOrderDO updateOrder = new AmazonOrderDO();
            updateOrder.setId(orderId);
            updateOrder.setItemCount(items.size());
            orderMapper.updateById(updateOrder);
        }
    }

    /**
     * 将 SP-API 订单明细 JSON 映射为 AmazonOrderItemDO。
     */
    private AmazonOrderItemDO mapOrderItemToDO(JsonNode itemNode, Long orderId) {
        AmazonOrderItemDO itemDO = new AmazonOrderItemDO();
        itemDO.setOrderId(orderId);
        itemDO.setAsin(getTextValue(itemNode, "ASIN"));
        itemDO.setSku(getTextValue(itemNode, "SellerSKU"));
        itemDO.setTitle(getTextValue(itemNode, "Title"));

        // 订购数量
        JsonNode quantityNode = itemNode.get("QuantityOrdered");
        if (quantityNode != null && !quantityNode.isNull()) {
            itemDO.setQuantity(quantityNode.asInt());
        }

        // 商品单价
        JsonNode itemPriceNode = itemNode.get("ItemPrice");
        if (itemPriceNode != null) {
            String amountStr = getTextValue(itemPriceNode, "Amount");
            if (amountStr != null) {
                itemDO.setPrice(new BigDecimal(amountStr));
            }
            itemDO.setCurrency(getTextValue(itemPriceNode, "CurrencyCode"));
        }

        return itemDO;
    }

    /**
     * 构建订单查询参数。
     */
    private Map<String, String> buildOrderQueryParams(String marketplaceId,
                                                       String createdAfter,
                                                       String nextToken) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MarketplaceIds", marketplaceId);
        params.put("CreatedAfter", createdAfter);
        params.put("OrderStatuses", "All");
        if (nextToken != null && !nextToken.isEmpty()) {
            params.put("NextToken", nextToken);
        }
        return params;
    }

    /**
     * 安全地从 JsonNode 中获取文本值。
     *
     * @param node      JSON 节点
     * @param fieldName 字段名
     * @return 字段文本值，不存在或为 null 时返回 null
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        return null;
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
    String resolveAwsRegion(String marketplaceId) {
        if (marketplaceId == null) {
            log.warn("[OrderSync] marketplaceId 为空, 默认使用 us-east-1");
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
        log.warn("[OrderSync] 未识别的 marketplaceId={}, 默认使用 us-east-1", marketplaceId);
        return "us-east-1";
    }
}
