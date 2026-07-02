package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonBsrRegressionDO;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonBsrRegressionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * BSR-销量估算实现。
 * <p>使用幂律回归模型：sales = a * bsr^alpha</p>
 *
 * @author AmazonOps AI
 */
@Service
public class BsrSalesEstimatorServiceImpl implements BsrSalesEstimatorService {

    @Resource
    private AmazonBsrRegressionMapper regressionMapper;

    @Override
    public Integer estimateMonthlySales(String categoryId, String marketplaceId, int bsr) {
        var regression = regressionMapper.selectByCategoryAndMarketplace(categoryId, marketplaceId);
        if (regression == null || regression.getCoefficientA() == null) {
            // 使用默认经验公式
            return estimateDefault(bsr);
        }
        double a = regression.getCoefficientA().doubleValue();
        double alpha = regression.getCoefficientAlpha().doubleValue();
        double sales = a * Math.pow(bsr, alpha);
        return (int) Math.round(sales);
    }

    /**
     * 默认经验公式：sales ≈ 30000 * bsr^(-0.7)
     */
    private int estimateDefault(int bsr) {
        double sales = 30000.0 * Math.pow(bsr, -0.7);
        return (int) Math.round(sales);
    }
}
