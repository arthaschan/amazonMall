package cn.iocoder.yudao.module.amazon.research.service;

/**
 * BSR-销量估算服务。
 * <p>基于幂律回归模型 sales = a * bsr^alpha 估算月销量。</p>
 *
 * @author AmazonOps AI
 */
public interface BsrSalesEstimatorService {

    /**
     * 根据 BSR 估算月销量。
     *
     * @param categoryId    类目 ID
     * @param marketplaceId 站点 ID
     * @param bsr           BSR 排名
     * @return 预估月销量
     */
    Integer estimateMonthlySales(String categoryId, String marketplaceId, int bsr);
}
