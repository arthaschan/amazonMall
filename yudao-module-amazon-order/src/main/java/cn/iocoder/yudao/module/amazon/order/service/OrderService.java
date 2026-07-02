package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;

/**
 * 订单管理 Service。
 *
 * @author AmazonOps AI
 */
public interface OrderService {

    AmazonOrderDO getOrder(Long id);

    AmazonOrderDO getOrderByAmazonOrderId(String amazonOrderId);

    PageResult<AmazonOrderDO> getOrderPage(OrderPageReqVO reqVO);
}
