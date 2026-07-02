package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.FbaShipmentPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonFbaShipmentDO;

/**
 * FBA 货件管理 Service。
 *
 * @author AmazonOps AI
 */
public interface FbaShipmentService {

    AmazonFbaShipmentDO getShipment(Long id);

    PageResult<AmazonFbaShipmentDO> getShipmentPage(FbaShipmentPageReqVO reqVO);

    void syncShipments(Long shopId);
}
