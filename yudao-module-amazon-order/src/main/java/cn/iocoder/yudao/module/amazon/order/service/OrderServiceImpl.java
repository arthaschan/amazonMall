package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class OrderServiceImpl implements OrderService {

    @Resource
    private AmazonOrderMapper orderMapper;

    @Override
    public AmazonOrderDO getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public AmazonOrderDO getOrderByAmazonOrderId(String amazonOrderId) {
        return orderMapper.selectByAmazonOrderId(amazonOrderId);
    }

    @Override
    public PageResult<AmazonOrderDO> getOrderPage(OrderPageReqVO reqVO) {
        return orderMapper.selectPage(reqVO);
    }
}
