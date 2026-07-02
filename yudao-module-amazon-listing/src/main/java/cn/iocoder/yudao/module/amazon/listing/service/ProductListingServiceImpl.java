package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.listing.controller.admin.vo.ProductPageReqVO;
import cn.iocoder.yudao.module.amazon.listing.controller.admin.vo.ProductSaveReqVO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 产品 Listing 管理 Service 实现。
 *
 * @author AmazonOps AI
 */
@Service
@Validated
public class ProductListingServiceImpl implements ProductListingService {

    @Resource
    private AmazonProductMapper productMapper;

    @Override
    public Long createProduct(ProductSaveReqVO reqVO) {
        var product = BeanUtils.toBean(reqVO, AmazonProductDO.class);
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public void updateProduct(ProductSaveReqVO reqVO) {
        productMapper.updateById(BeanUtils.toBean(reqVO, AmazonProductDO.class));
    }

    @Override
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

    @Override
    public AmazonProductDO getProduct(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public PageResult<AmazonProductDO> getProductPage(ProductPageReqVO reqVO) {
        return productMapper.selectPage(reqVO);
    }
}
