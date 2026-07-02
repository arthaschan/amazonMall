package cn.iocoder.yudao.module.amazon.order.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.FbaShipmentPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonFbaShipmentDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonFbaShipmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class FbaShipmentServiceImpl implements FbaShipmentService {

    @Resource
    private AmazonFbaShipmentMapper shipmentMapper;

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
        // TODO: 调用 SP-API FBA Inbound API 同步货件
    }
}
