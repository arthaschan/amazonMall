package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.listing.controller.admin.vo.ProductPageReqVO;
import cn.iocoder.yudao.module.amazon.listing.controller.admin.vo.ProductSaveReqVO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;

import javax.validation.Valid;

/**
 * 产品 Listing 管理 Service。
 *
 * @author AmazonOps AI
 */
public interface ProductListingService {

    Long createProduct(@Valid ProductSaveReqVO reqVO);

    void updateProduct(@Valid ProductSaveReqVO reqVO);

    void deleteProduct(Long id);

    AmazonProductDO getProduct(Long id);

    PageResult<AmazonProductDO> getProductPage(ProductPageReqVO reqVO);
}
