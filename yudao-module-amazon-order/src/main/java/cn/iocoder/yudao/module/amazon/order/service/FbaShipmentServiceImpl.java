package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.FbaShipmentPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonFbaShipmentDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonFbaShipmentMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * FBA 货件管理与同步服务实现。
 *
 * <p>通过 SP-API Fulfillment Inbound API v0 拉取 FBA 入库货件信息，
 * 映射为 {@link AmazonFbaShipmentDO} 并持久化。</p>
 *
 * <p>API 路径: {@code GET /fba/inbound/v0/shipments}
 * 参数: QueryType=SHIPMENT, MarketplaceId=xxx</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class FbaShipmentServiceImpl implements FbaShipmentService {

    /** SP-API FBA 入库货件接口路径 */
    private static final String SHIPMENTS_PATH = "/fba/inbound/v0/shipments";

    @Resource
    private AmazonFbaShipmentMapper shipmentMapper;

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonShopService amazonShopService;

    @Override
    public AmazonFbaShipmentDO getShipment(Long id) {
        return shipmentMapper.selectById(id);
    }

    @Override
    public PageResult<AmazonFbaShipmentDO> getShipmentPage(FbaShipmentPageReqVO reqVO) {
        return shipmentMapper.selectPage(reqVO);
    }

    @Override
    public void syncShipments(Long shopId) {
        log.info("[FbaShipment] 开始同步 FBA 货件 shopId={}", shopId);

        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            log.error("[FbaShipment] 店铺不存在 shopId={}", shopId);
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }

        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(shop.getMarketplaceId());
        if (marketplace == null) {
            log.error("[FbaShipment] 不支持的 marketplaceId={}", shop.getMarketplaceId());
            throw new IllegalArgumentException("Unsupported marketplaceId: " + shop.getMarketplaceId());
        }

        String sellerId = shop.getSellerId();
        String awsRegion = marketplace.getAwsRegion();

        int totalSynced = 0;
        String nextToken = null;

        do {
            Map<String, String> queryParams = buildShipmentQueryParams(shop.getMarketplaceId(), nextToken);

            SpApiClient.SpApiResponse response;
            try {
                response = spApiClient.getFullResponse(sellerId, awsRegion, SHIPMENTS_PATH, queryParams);
            } catch (Exception e) {
                log.error("[FbaShipment] SP-API 货件查询失败 shopId={}", shopId, e);
                break;
            }

            if (!response.isSuccessful() || response.getJsonBody() == null) {
                log.error("[FbaShipment] SP-API 货件查询返回错误 statusCode={}, error={}",
                        response.getStatusCode(), response.getErrorMessage());
                break;
            }

            JsonNode jsonBody = response.getJsonBody();
            JsonNode payload = jsonBody.get("payload");
            if (payload == null) {
                log.warn("[FbaShipment] 响应中缺少 payload 节点");
                break;
            }

            // 解析分页 Token
            JsonNode nextTokenNode = payload.get("NextToken");
            nextToken = (nextTokenNode != null && !nextTokenNode.isNull()) ? nextTokenNode.asText() : null;

            // 解析货件列表
            JsonNode shipmentData = payload.get("ShipmentData");
            if (shipmentData == null || !shipmentData.isArray() || shipmentData.isEmpty()) {
                log.info("[FbaShipment] 当前页无货件数据, 结束同步");
                break;
            }

            for (JsonNode shipmentNode : shipmentData) {
                try {
                    AmazonFbaShipmentDO shipment = mapShipmentToDO(shipmentNode, shop);
                    if (shipment != null) {
                        persistShipment(shipment);
                        totalSynced++;
                    }
                } catch (Exception e) {
                    String shipmentId = getTextValue(shipmentNode, "ShipmentId");
                    log.error("[FbaShipment] 处理货件失败 shipmentId={}", shipmentId, e);
                }
            }

            log.info("[FbaShipment] 当前页处理完成, 本页货件数={}, 累计={}",
                    shipmentData.size(), totalSynced);

        } while (nextToken != null && !nextToken.isEmpty());

        log.info("[FbaShipment] FBA 货件同步完成 shopId={}, totalSynced={}", shopId, totalSynced);
    }

    /**
     * 将 SP-API 货件 JSON 映射为 AmazonFbaShipmentDO。
     */
    private AmazonFbaShipmentDO mapShipmentToDO(JsonNode node, AmazonShopDO shop) {
        String shipmentId = getTextValue(node, "ShipmentId");
        if (shipmentId == null || shipmentId.trim().isEmpty()) {
            log.warn("[FbaShipment] 货件缺少 ShipmentId, 跳过");
            return null;
        }

        AmazonFbaShipmentDO shipment = new AmazonFbaShipmentDO();
        shipment.setShopId(shop.getId());
        shipment.setTenantId(shop.getTenantId());
        shipment.setShipmentId(shipmentId);
        shipment.setShipmentName(getTextValue(node, "ShipmentName"));
        shipment.setStatus(getTextValue(node, "ShipmentStatus"));
        shipment.setDestinationFc(getTextValue(node, "DestinationFulfillmentCenterId"));

        // 发货地址
        JsonNode shipFromNode = node.get("ShipFromAddress");
        if (shipFromNode != null) {
            String city = getTextValue(shipFromNode, "City");
            String state = getTextValue(shipFromNode, "StateOrRegion");
            String country = getTextValue(shipFromNode, "CountryCode");
            StringBuilder address = new StringBuilder();
            if (city != null) address.append(city);
            if (state != null) {
                if (address.length() > 0) address.append(", ");
                address.append(state);
            }
            if (country != null) {
                if (address.length() > 0) address.append(", ");
                address.append(country);
            }
            if (address.length() > 0) {
                shipment.setShipFromAddress(address.toString());
            }
        }

        // 标签数量
        JsonNode labelPrepNode = node.get("LabelPrepType");
        if (labelPrepNode != null && !labelPrepNode.isNull()) {
            // LabelPrepType 不是数量，这里记录为 null；实际标签数在货件项中
            shipment.setLabelCount(null);
        }

        return shipment;
    }

    /**
     * 持久化货件：根据 shopId + shipmentId 查询已有记录，存在则更新，不存在则新增。
     */
    private void persistShipment(AmazonFbaShipmentDO shipment) {
        AmazonFbaShipmentDO existing = shipmentMapper.selectOne(
                new LambdaQueryWrapper<AmazonFbaShipmentDO>()
                        .eq(AmazonFbaShipmentDO::getShopId, shipment.getShopId())
                        .eq(AmazonFbaShipmentDO::getShipmentId, shipment.getShipmentId())
                        .last("LIMIT 1"));

        if (existing != null) {
            shipment.setId(existing.getId());
            shipmentMapper.updateById(shipment);
        } else {
            shipmentMapper.insert(shipment);
        }
    }

    /**
     * 构建货件查询参数。
     */
    private Map<String, String> buildShipmentQueryParams(String marketplaceId, String nextToken) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("QueryType", "SHIPMENT");
        params.put("MarketplaceId", marketplaceId);
        if (nextToken != null && !nextToken.isEmpty()) {
            params.put("NextToken", nextToken);
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
