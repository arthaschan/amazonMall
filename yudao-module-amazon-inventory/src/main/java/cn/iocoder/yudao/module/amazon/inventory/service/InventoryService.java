package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.InventoryPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;

import java.util.List;

/**
 * 库存管理 Service。
 *
 * @author AmazonOps AI
 */
public interface InventoryService {

    PageResult<AmazonInventoryDO> getInventoryPage(InventoryPageReqVO reqVO);

    List<AmazonInventoryDO> getByAsin(Long shopId, String asin);

    /** 从 SP-API 同步库存 */
    void syncInventory(Long shopId);
}
