package cn.iocoder.yudao.module.amazon.shop.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopCreateReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopUpdateReqVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;

import java.util.List;

/**
 * Amazon Shop service interface.
 *
 * <p>Provides CRUD operations, OAuth authorisation flow, token management,
 * and data sync triggers for Amazon seller accounts.
 *
 * @author AmazonOps AI
 */
public interface AmazonShopService {

    /**
     * Creates a new shop record.
     *
     * @param createReqVO the creation request
     * @return the new shop ID
     */
    Long createShop(AmazonShopCreateReqVO createReqVO);

    /**
     * Updates an existing shop record.
     *
     * @param updateReqVO the update request
     */
    void updateShop(AmazonShopUpdateReqVO updateReqVO);

    /**
     * Soft-deletes a shop record.
     *
     * @param id the shop ID
     */
    void deleteShop(Long id);

    /**
     * Gets a shop by ID.
     *
     * @param id the shop ID
     * @return the shop, or {@code null} if not found
     */
    AmazonShopDO getShopById(Long id);

    /**
     * Gets a shop by seller ID and marketplace ID.
     *
     * @param sellerId      the Amazon seller ID
     * @param marketplaceId the marketplace ID
     * @return the shop, or {@code null} if not found
     */
    AmazonShopDO getShopBySellerAndMarketplace(String sellerId, String marketplaceId);

    /**
     * Lists all shops for the current tenant.
     *
     * @return list of shops
     */
    List<AmazonShopDO> listShops();

    /**
     * Paginated query of shops.
     *
     * @param pageReqVO the page request with filters
     * @return paginated result
     */
    PageResult<AmazonShopDO> getShopPage(AmazonShopPageReqVO pageReqVO);

    /**
     * Starts the OAuth authorisation flow for a shop.
     * Generates the Amazon SP-API authorisation URL that the seller
     * should be redirected to.
     *
     * @param id the shop ID
     * @return the OAuth authorisation URL
     */
    String authorizeShop(Long id);

    /**
     * Handles the OAuth callback after the seller authorises the application.
     * Exchanges the authorisation code for tokens and stores them encrypted.
     *
     * @param spapiOauthCode  the OAuth authorisation code from the callback
     * @param sellingPartnerId the Amazon selling partner ID
     * @param state           the state parameter (shop ID encoded)
     */
    void handleAuthCallback(String spapiOauthCode, String sellingPartnerId, String state);

    /**
     * Manually triggers a token refresh for the specified shop.
     *
     * @param id the shop ID
     */
    void refreshToken(Long id);

    /**
     * Triggers a full data sync for the specified shop.
     * This is an async operation that enqueues sync tasks for
     * orders, products, inventory, etc.
     *
     * @param id the shop ID
     */
    void syncShopData(Long id);

    /**
     * Gets the decrypted refresh token for internal API calls.
     * Should only be used by the SP-API client layer.
     *
     * @param id the shop ID
     * @return the plaintext refresh token
     */
    String getDecryptedRefreshToken(Long id);

    /**
     * Gets the decrypted access token for internal API calls.
     * Should only be used by the SP-API client layer.
     *
     * @param id the shop ID
     * @return the plaintext access token, or null if not available
     */
    String getDecryptedAccessToken(Long id);
}
