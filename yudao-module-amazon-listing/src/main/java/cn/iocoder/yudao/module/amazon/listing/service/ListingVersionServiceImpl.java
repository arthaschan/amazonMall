package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonListingVersionMapper;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Listing 版本管理实现。
 *
 * @author AmazonOps AI
 */
@Service
public class ListingVersionServiceImpl implements ListingVersionService {

    @Resource
    private AmazonListingVersionMapper versionMapper;

    @Resource
    private AmazonProductMapper productMapper;

    @Override
    public List<AmazonListingVersionDO> getVersions(Long productId) {
        return versionMapper.selectByProductId(productId);
    }

    @Override
    public AmazonListingVersionDO getVersion(Long versionId) {
        return versionMapper.selectById(versionId);
    }

    @Override
    public void rollback(Long productId, Integer versionNum) {
        List<AmazonListingVersionDO> versions = versionMapper.selectByProductId(productId);
        java.util.Optional<AmazonListingVersionDO> target = versions.stream();
                .filter(v -> v.getVersionNum().equals(versionNum))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionNum));

        AmazonProductDO product = new AmazonProductDO();
        product.setId(productId);
        product.setTitle(target.getTitle());
        product.setBulletPoints(target.getBulletPoints());
        product.setDescription(target.getDescription());
        product.setBackendKeywords(target.getBackendKeywords());
        productMapper.updateById(product);
    }
}
