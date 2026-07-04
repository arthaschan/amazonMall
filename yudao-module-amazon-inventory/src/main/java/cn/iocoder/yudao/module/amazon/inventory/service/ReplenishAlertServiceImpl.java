package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.ReplenishAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonReplenishAlertDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonReplenishAlertMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReplenishAlertServiceImpl implements ReplenishAlertService {

    @Resource
    private AmazonReplenishAlertMapper alertMapper;

    @Override
    public PageResult<AmazonReplenishAlertDO> getAlertPage(ReplenishAlertPageReqVO reqVO) {
        return alertMapper.selectPage(reqVO);
    }

    @Override
    public List<AmazonReplenishAlertDO> getUnacknowledgedAlerts(Long shopId) {
        return alertMapper.selectUnacknowledged(shopId);
    }

    @Override
    public void acknowledgeAlert(Long alertId) {
        AmazonReplenishAlertDO alert = alertMapper.selectById(alertId);
        if (alert != null) {
            alert.setAcknowledged(true);
            alertMapper.updateById(alert);
        }
    }

    @Override
    public void scanAndAlert(Long shopId) {
        // TODO: 扫描库存，对比再订购点，生成预警
    }
}
