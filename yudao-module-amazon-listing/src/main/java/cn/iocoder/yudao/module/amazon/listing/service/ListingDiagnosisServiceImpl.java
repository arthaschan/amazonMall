package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Listing 诊断服务实现。
 *
 * @author AmazonOps AI
 */
@Service
public class ListingDiagnosisServiceImpl implements ListingDiagnosisService {

    @Resource
    private AmazonProductMapper productMapper;

    @Override
    public Map<String, Object> diagnose(Long productId) {
        AmazonProductDO product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        diagnoseTitle(product, result);
        diagnoseBulletPoints(product, result);
        diagnosePrice(product, result);
        diagnoseRating(product, result);
        return result;
    }

    private void diagnoseTitle(AmazonProductDO product, Map<String, Object> result) {
        String title = product.getTitle();
        if (title == null || title.length() < 50) {
            result.put("title", "标题过短，建议 80-200 字符，包含核心关键词");
        } else if (title.length() > 200) {
            result.put("title", "标题过长，建议控制在 200 字符以内");
        } else {
            result.put("title", "标题长度合理");
        }
    }

    private void diagnoseBulletPoints(AmazonProductDO product, Map<String, Object> result) {
        List<String> bullets = product.getBulletPoints();
        if (bullets == null || bullets.size() < 5) {
            result.put("bulletPoints", "五点描述不足 5 条，建议补全");
        } else {
            result.put("bulletPoints", "五点描述数量达标");
        }
    }

    private void diagnosePrice(AmazonProductDO product, Map<String, Object> result) {
        if (product.getPrice() == null) {
            result.put("price", "未设置价格");
        } else {
            result.put("price", "价格已设置: " + product.getPrice());
        }
    }

    private void diagnoseRating(AmazonProductDO product, Map<String, Object> result) {
        if (product.getRating() == null) {
            result.put("rating", "暂无评分数据");
        } else if (product.getRating().doubleValue() < 4.0) {
            result.put("rating", "评分偏低 (" + product.getRating() + ")，建议优化产品质量或客服");
        } else {
            result.put("rating", "评分良好 (" + product.getRating() + ")");
        }
    }
}
