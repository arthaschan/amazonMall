package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;

import java.util.Map;

/**
 * Listing 诊断服务。
 * <p>从标题、图片、五点、A+、关键词、价格等维度诊断 Listing 问题。</p>
 *
 * @author AmazonOps AI
 */
public interface ListingDiagnosisService {

    /**
     * 对指定产品进行 Listing 诊断。
     *
     * @param productId 产品 ID
     * @return 诊断结果，key=维度，value=诊断结论
     */
    Map<String, Object> diagnose(Long productId);
}
