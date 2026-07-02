package cn.iocoder.yudao.module.amazon.shop.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopCreateReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopRespVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopUpdateReqVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * Amazon Shop management controller.
 *
 * <p>Provides REST endpoints for CRUD operations on Amazon seller accounts,
 * OAuth authorisation flow, token refresh, and data sync triggers.
 *
 * @author AmazonOps AI
 */
@Tag(name = "Management Console - Amazon Shop")
@RestController
@RequestMapping("/amazon/shop")
@Validated
public class AmazonShopController {

    @Resource
    private AmazonShopService shopService;

    // ── CRUD ──────────────────────────────────────────────────────────────

    @PostMapping("/create")
    @Operation(summary = "Create a new Amazon shop")
    @PreAuthorize("@ss.hasPermission('amazon:shop:create')")
    public CommonResult<Long> createShop(@Valid @RequestBody AmazonShopCreateReqVO createReqVO) {
        Long shopId = shopService.createShop(createReqVO);
        return success(shopId);
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing Amazon shop")
    @PreAuthorize("@ss.hasPermission('amazon:shop:update')")
    public CommonResult<Boolean> updateShop(@Valid @RequestBody AmazonShopUpdateReqVO updateReqVO) {
        shopService.updateShop(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete an Amazon shop (soft delete)")
    @Parameter(name = "id", description = "Shop ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('amazon:shop:delete')")
    public CommonResult<Boolean> deleteShop(@RequestParam("id") Long id) {
        shopService.deleteShop(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get Amazon shop details")
    @Parameter(name = "id", description = "Shop ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('amazon:shop:query')")
    public CommonResult<AmazonShopRespVO> getShop(@RequestParam("id") Long id) {
        AmazonShopDO shop = shopService.getShopById(id);
        AmazonShopRespVO respVO = BeanUtils.toBean(shop, AmazonShopRespVO.class);
        // Set credentialConfigured flag
        if (respVO != null && shop != null) {
            respVO.setCredentialConfigured(
                    shop.getRefreshToken() != null && !shop.getRefreshToken().isBlank());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "Get paginated list of Amazon shops")
    @PreAuthorize("@ss.hasPermission('amazon:shop:query')")
    public CommonResult<PageResult<AmazonShopRespVO>> getShopPage(@Valid AmazonShopPageReqVO pageReqVO) {
        PageResult<AmazonShopDO> pageResult = shopService.getShopPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AmazonShopRespVO.class));
    }

    // ── OAuth Authorisation ───────────────────────────────────────────────

    @GetMapping("/authorize/{id}")
    @Operation(summary = "Get OAuth authorisation URL for a shop",
            description = "Returns the Amazon Seller Central authorisation URL that the seller should be redirected to")
    @Parameter(name = "id", description = "Shop ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('amazon:shop:update')")
    public CommonResult<String> authorizeShop(@PathVariable("id") Long id) {
        String authUrl = shopService.authorizeShop(id);
        return success(authUrl);
    }

    @PostMapping("/callback")
    @Operation(summary = "Handle OAuth callback from Amazon",
            description = "Processes the OAuth callback after the seller authorises the application")
    public CommonResult<Boolean> handleAuthCallback(
            @RequestParam("spapi_oauth_code") String spapiOauthCode,
            @RequestParam("selling_partner_id") String sellingPartnerId,
            @RequestParam("state") String state) {
        shopService.handleAuthCallback(spapiOauthCode, sellingPartnerId, state);
        return success(true);
    }

    // ── Token Management ──────────────────────────────────────────────────

    @PostMapping("/refresh-token/{id}")
    @Operation(summary = "Manually refresh the access token for a shop")
    @Parameter(name = "id", description = "Shop ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('amazon:shop:update')")
    public CommonResult<Boolean> refreshToken(@PathVariable("id") Long id) {
        shopService.refreshToken(id);
        return success(true);
    }

    // ── Data Sync ─────────────────────────────────────────────────────────

    @PostMapping("/sync/{id}")
    @Operation(summary = "Trigger full data sync for a shop",
            description = "Enqueues async sync tasks for orders, products, inventory, etc.")
    @Parameter(name = "id", description = "Shop ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('amazon:shop:update')")
    public CommonResult<Boolean> syncShopData(@PathVariable("id") Long id) {
        shopService.syncShopData(id);
        return success(true);
    }
}
