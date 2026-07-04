package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonListingVersionMapper;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI Listing 生成服务实现。
 *
 * @author AmazonOps AI
 */
@Service
public class AiListingGeneratorServiceImpl implements AiListingGeneratorService {

    @Resource
    private AmazonProductMapper productMapper;

    @Resource
    private AmazonListingVersionMapper versionMapper;

    @Override
    public AmazonListingVersionDO generate(Long productId, String prompt) {
        AmazonProductDO product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        // TODO: 调用 AI 模型生成 Listing 内容
        AmazonListingVersionDO version = buildNewVersion(product, true);
        version.setAiScore(new BigDecimal("85"));
        version.setChangeSummary("AI 全新生成: " + prompt);
        versionMapper.insert(version);
        return version;
    }

    @Override
    public AmazonListingVersionDO optimize(Long productId, String prompt) {
        AmazonProductDO product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        // TODO: 调用 AI 模型优化现有 Listing
        AmazonListingVersionDO version = buildNewVersion(product, true);
        version.setAiScore(new BigDecimal("90"));
        version.setChangeSummary("AI 优化: " + prompt);
        versionMapper.insert(version);
        return version;
    }

    private AmazonListingVersionDO buildNewVersion(AmazonProductDO product, boolean aiGenerated) {
        List<AmazonListingVersionDO> versions = versionMapper.selectByProductId(product.getId());
        int nextVersion = versions.isEmpty() ? 1 : versions.stream()
                .mapToInt(AmazonListingVersionDO::getVersionNum).max().orElse(0) + 1;

        AmazonListingVersionDO version = new AmazonListingVersionDO();
        version.setProductId(product.getId());
        version.setVersionNum(nextVersion);
        version.setTitle(product.getTitle());
        version.setBulletPoints(product.getBulletPoints());
        version.setDescription(product.getDescription());
        version.setBackendKeywords(product.getBackendKeywords());
        version.setAiGenerated(aiGenerated);
        return version;
    }
}
