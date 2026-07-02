package cn.iocoder.yudao.module.amazon.inventory.service;

import java.util.List;

/**
 * 滞销检测服务。
 * <p>识别长期不动销或周转极慢的库存。</p>
 *
 * @author AmazonOps AI
 */
public interface SlowMovingDetectorService {

    /**
     * 检测滞销品。
     *
     * @param shopId          店铺 ID
     * @param noSaleThreshold 无销售天数阈值
     * @return 滞销 ASIN 列表
     */
    List<String> detectSlowMoving(Long shopId, int noSaleThreshold);
}
