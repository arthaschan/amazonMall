package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonSupplierMatchDO;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonSupplierMatchMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商匹配服务实现。
 *
 * @author AmazonOps AI
 */
@Service
public class SupplierMatchServiceImpl implements SupplierMatchService {

    @Resource
    private AmazonSupplierMatchMapper supplierMatchMapper;

    @Override
    public List<AmazonSupplierMatchDO> matchSuppliers(Long opportunityId) {
        // TODO: 集成 1688 / Alibaba API 搜索供应商
        return getByOpportunityId(opportunityId);
    }

    @Override
    public List<AmazonSupplierMatchDO> getByOpportunityId(Long opportunityId) {
        return supplierMatchMapper.selectByOpportunityId(opportunityId);
    }
}
