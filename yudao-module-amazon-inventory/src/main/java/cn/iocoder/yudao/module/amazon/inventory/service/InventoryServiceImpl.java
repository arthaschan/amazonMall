package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.InventoryPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Resource
    private AmazonInventoryMapper inventoryMapper;

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
        // TODO: 调用 SP-API FBA Inventory API
    }
}
