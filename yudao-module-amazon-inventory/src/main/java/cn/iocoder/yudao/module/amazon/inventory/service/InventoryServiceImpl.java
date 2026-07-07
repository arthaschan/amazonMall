package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.InventoryPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    @Resource
    private AmazonInventoryMapper inventoryMapper;

    @Resource
    private AmazonShopService amazonShopService;

    @Resource
    private InventorySyncService inventorySyncService;

    @Override
    public PageResult<AmazonInventoryDO> getInventoryPage(InventoryPageReqVO reqVO) {
        return inventoryMapper.selectPage(reqVO);
    }

    @Override
    public List<AmazonInventoryDO> getByAsin(Long shopId, String asin) {
        return inventoryMapper.selectByAsin(shopId, asin);
    }

    @Override
    public void syncInventory(Long shopId) {
        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            log.error("[Inventory] 店铺不存在 shopId={}", shopId);
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
        log.info("[Inventory] 委托库存同步服务 shopId={}, marketplaceId={}", shopId, shop.getMarketplaceId());
        inventorySyncService.syncInventory(shopId, shop.getMarketplaceId());
    }
}
