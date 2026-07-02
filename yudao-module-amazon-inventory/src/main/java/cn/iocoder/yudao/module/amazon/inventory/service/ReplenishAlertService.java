package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.ReplenishAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonReplenishAlertDO;

import java.util.List;

/**
 * 补货预警 Service。
 *
 * @author AmazonOps AI
 */
public interface ReplenishAlertService {

    PageResult<AmazonReplenishAlertDO> getAlertPage(ReplenishAlertPageReqVO reqVO);

    List<AmazonReplenishAlertDO> getUnacknowledgedAlerts(Long shopId);

    void acknowledgeAlert(Long alertId);

    /** 扫描并生成补货预警 */
    void scanAndAlert(Long shopId);
}
