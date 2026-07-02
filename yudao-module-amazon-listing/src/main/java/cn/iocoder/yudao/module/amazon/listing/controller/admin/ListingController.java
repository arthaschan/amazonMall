package cn.iocoder.yudao.module.amazon.listing.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.listing.controller.admin.vo.*;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.service.ProductListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - Listing 管理")
@RestController
@RequestMapping("/amazon/listing/product")
@Validated
public class ListingController {

    @Resource
    private ProductListingService productListingService;

    @PostMapping("/create")
    @Operation(summary = "创建产品")
    @PreAuthorize("@ss.hasPermission('amazon:listing:product:create')")
    public CommonResult<Long> createProduct(@Valid @RequestBody ProductSaveReqVO reqVO) {
        return success(productListingService.createProduct(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新产品")
    @PreAuthorize("@ss.hasPermission('amazon:listing:product:update')")
    public CommonResult<Boolean> updateProduct(@Valid @RequestBody ProductSaveReqVO reqVO) {
        productListingService.updateProduct(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产品")
    @Parameter(name = "id", description = "产品 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:listing:product:delete')")
    public CommonResult<Boolean> deleteProduct(@RequestParam Long id) {
        productListingService.deleteProduct(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取产品详情")
    @PreAuthorize("@ss.hasPermission('amazon:listing:product:query')")
    public CommonResult<ProductRespVO> getProduct(@RequestParam Long id) {
        var product = productListingService.getProduct(id);
        return success(BeanUtils.toBean(product, ProductRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "产品分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:listing:product:query')")
    public CommonResult<PageResult<ProductRespVO>> getProductPage(@Valid ProductPageReqVO reqVO) {
        var page = productListingService.getProductPage(reqVO);
        return success(BeanUtils.toBean(page, ProductRespVO.class));
    }
}
