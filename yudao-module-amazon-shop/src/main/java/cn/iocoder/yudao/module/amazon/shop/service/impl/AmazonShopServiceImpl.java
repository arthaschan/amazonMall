package cn.iocoder.yudao.module.amazon.shop.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopCreateReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopUpdateReqVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.dal.mysql.AmazonShopMapper;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonShopStatusEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import cn.iocoder.yudao.module.amazon.common.sync.ShopSyncTriggeredEvent;
import com.yudao.module.amazon.common.core.AmazonCredentialEncryptor;
import com.yudao.module.amazon.common.core.AmazonProperties;
import com.yudao.module.amazon.common.core.SpApiTokenRefresher;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.amazon.shop.enums.ErrorCodeConstants.*;

/**
 * Amazon Shop service implementation.
 *
 * <p>Handles CRUD, OAuth flow, token management, and data sync triggers
 * for Amazon seller accounts. All credential fields are encrypted with
 * AES-256-GCM before persistence via {@link AmazonCredentialEncryptor}.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@Validated
public class AmazonShopServiceImpl implements AmazonShopService {

    @Resource
    private AmazonShopMapper shopMapper;

    @Resource
    private AmazonCredentialEncryptor credentialEncryptor;

    @Resource
    private AmazonProperties amazonProperties;

    @Resource
    private SpApiTokenRefresher tokenRefresher;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Override
    public Long createShop(AmazonShopCreateReqVO createReqVO) {
        // Validate marketplace ID
        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(createReqVO.getMarketplaceId());
        if (marketplace == null) {
            throw exception(SHOP_MARKETPLACE_INVALID);
        }

        // Check for duplicate seller ID + marketplace combination
        AmazonShopDO existing = shopMapper.selectBySellerIdAndMarketplaceId(
                createReqVO.getSellerId(), createReqVO.getMarketplaceId());
        if (existing != null) {
            throw exception(SHOP_SELLER_MARKETPLACE_DUPLICATE);
        }

        // Build DO
        AmazonShopDO shop = BeanUtils.toBean(createReqVO, AmazonShopDO.class);

        // Encrypt sensitive credentials before persistence
        encryptCredentials(shop, createReqVO.getClientId(),
                createReqVO.getClientSecret(), createReqVO.getRefreshToken());

        shopMapper.insert(shop);
        log.info("Created Amazon shop [id={}, name={}, marketplace={}, seller={}]",
                shop.getId(), shop.getShopName(), shop.getMarketplaceId(), shop.getSellerId());
        return shop.getId();
    }

    @Override
    public void updateShop(AmazonShopUpdateReqVO updateReqVO) {
        // Validate existence
        AmazonShopDO existing = validateShopExists(updateReqVO.getId());

        // Build update DO
        AmazonShopDO updateDO = BeanUtils.toBean(updateReqVO, AmazonShopDO.class);

        // Encrypt credentials if they are being updated
        if (updateReqVO.getClientId() != null || updateReqVO.getClientSecret() != null
                || updateReqVO.getRefreshToken() != null) {
            encryptCredentials(updateDO,
                    updateReqVO.getClientId() != null ? updateReqVO.getClientId() : null,
                    updateReqVO.getClientSecret() != null ? updateReqVO.getClientSecret() : null,
                    updateReqVO.getRefreshToken() != null ? updateReqVO.getRefreshToken() : null);
        }

        shopMapper.updateById(updateDO);
        log.info("Updated Amazon shop [id={}]", updateReqVO.getId());
    }

    @Override
    public void deleteShop(Long id) {
        validateShopExists(id);
        shopMapper.deleteById(id);
        log.info("Deleted Amazon shop [id={}]", id);
    }

    @Override
    public AmazonShopDO getShopById(Long id) {
        return shopMapper.selectById(id);
    }

    @Override
    public AmazonShopDO getShopBySellerAndMarketplace(String sellerId, String marketplaceId) {
        return shopMapper.selectBySellerIdAndMarketplaceId(sellerId, marketplaceId);
    }

    @Override
    public List<AmazonShopDO> listShops() {
        return shopMapper.selectList();
    }

    @Override
    public PageResult<AmazonShopDO> getShopPage(AmazonShopPageReqVO pageReqVO) {
        return shopMapper.selectPage(pageReqVO);
    }

    // ── OAuth Authorisation Flow ──────────────────────────────────────────

    @Override
    public String authorizeShop(Long id) {
        AmazonShopDO shop = validateShopExists(id);

        // Build the SP-API OAuth authorisation URL
        // Reference: https://developer-docs.amazon.com/sp-api/docs/authorizing-selling-partner-api-applications
        String authUrl = buildAuthorizeUrl(shop);

        // Update shop status to AUTH_PENDING
        shopMapper.updateById(new AmazonShopDO() {{
            setId(id);
            setStatus(AmazonShopStatusEnum.AUTH_PENDING.getStatus());
        }});

        log.info("Generated OAuth authorisation URL for shop [id={}, marketplace={}]",
                id, shop.getMarketplaceId());
        return authUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAuthCallback(String spapiOauthCode, String sellingPartnerId, String state) {
        log.info("Handling OAuth callback: sellingPartnerId={}, state={}", sellingPartnerId, state);

        // Decode state to get shop ID
        Long shopId;
        try {
            shopId = Long.parseLong(state);
        } catch (NumberFormatException e) {
            throw exception(SHOP_AUTH_CALLBACK_INVALID_STATE);
        }

        AmazonShopDO shop = validateShopExists(shopId);

        // Exchange the OAuth code for tokens
        SpApiTokenRefresher.TokenResponse tokenResponse;
        try {
            tokenResponse = tokenRefresher.exchangeAuthorizationCode(spapiOauthCode);
        } catch (Exception e) {
            log.error("Failed to exchange OAuth code for shop [id={}]: {}", shopId, e.getMessage());
            throw exception(SHOP_AUTH_TOKEN_EXCHANGE_FAILED);
        }

        // Encrypt and store the tokens
        AmazonShopDO updateDO = new AmazonShopDO();
        updateDO.setId(shopId);
        updateDO.setSellerId(sellingPartnerId);
        updateDO.setRefreshToken(credentialEncryptor.encrypt(tokenResponse.refreshToken()));
        updateDO.setAccessToken(credentialEncryptor.encrypt(tokenResponse.accessToken()));
        updateDO.setTokenExpireAt(LocalDateTime.now().plusSeconds(tokenResponse.expiresIn()));
        updateDO.setStatus(AmazonShopStatusEnum.ENABLED.getStatus());

        shopMapper.updateById(updateDO);
        log.info("OAuth authorisation completed for shop [id={}, sellerId={}]", shopId, sellingPartnerId);
    }

    // ── Token Management ──────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshToken(Long id) {
        AmazonShopDO shop = validateShopExists(id);

        if (shop.getRefreshToken() == null || shop.getRefreshToken().trim().isEmpty()) {
            throw exception(SHOP_REFRESH_TOKEN_MISSING);
        }

        // Decrypt the refresh token
        String plainRefreshToken = credentialEncryptor.decrypt(shop.getRefreshToken());

        // Call LWA to get new access token
        SpApiTokenRefresher.TokenResponse tokenResponse;
        try {
            tokenResponse = tokenRefresher.refreshAccessToken(plainRefreshToken);
        } catch (Exception e) {
            log.error("Token refresh failed for shop [id={}]: {}", id, e.getMessage());
            // Mark as auth expired
            shopMapper.updateById(new AmazonShopDO() {{
                setId(id);
                setStatus(AmazonShopStatusEnum.AUTH_EXPIRED.getStatus());
            }});
            throw exception(SHOP_TOKEN_REFRESH_FAILED);
        }

        // Update tokens
        AmazonShopDO updateDO = new AmazonShopDO();
        updateDO.setId(id);
        updateDO.setAccessToken(credentialEncryptor.encrypt(tokenResponse.accessToken()));
        updateDO.setTokenExpireAt(LocalDateTime.now().plusSeconds(tokenResponse.expiresIn()));
        updateDO.setStatus(AmazonShopStatusEnum.ENABLED.getStatus());

        // If a new refresh token was returned, update it too
        if (tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().trim().isEmpty()) {
            updateDO.setRefreshToken(credentialEncryptor.encrypt(tokenResponse.refreshToken()));
        }

        shopMapper.updateById(updateDO);
        log.info("Token refreshed successfully for shop [id={}]", id);
    }

    @Override
    public void syncShopData(Long id) {
        AmazonShopDO shop = validateShopExists(id);

        if (!AmazonShopStatusEnum.ENABLED.getStatus().equals(shop.getStatus())) {
            throw exception(SHOP_NOT_ENABLED);
        }

        log.info("[syncShopData] 发布店铺数据同步事件 shopId={}, marketplaceId={}, sellerId={}",
                id, shop.getMarketplaceId(), shop.getSellerId());

        // 发布 Spring 事件，各业务模块通过 @EventListener 监听并执行各自的同步逻辑
        // (订单、商品、库存、评论、广告、FBA 货件等)
        eventPublisher.publishEvent(new ShopSyncTriggeredEvent(
                this, id, shop.getMarketplaceId(), shop.getSellerId()));

        log.info("[syncShopData] 店铺数据同步事件已发布 shopId={}", id);
    }

    @Override
    public String getDecryptedRefreshToken(Long id) {
        AmazonShopDO shop = validateShopExists(id);
        if (shop.getRefreshToken() == null || shop.getRefreshToken().trim().isEmpty()) {
            return null;
        }
        return credentialEncryptor.decrypt(shop.getRefreshToken());
    }

    @Override
    public String getDecryptedAccessToken(Long id) {
        AmazonShopDO shop = validateShopExists(id);
        if (shop.getAccessToken() == null || shop.getAccessToken().trim().isEmpty()) {
            return null;
        }
        return credentialEncryptor.decrypt(shop.getAccessToken());
    }

    // ── Internal Helpers ──────────────────────────────────────────────────

    /**
     * Validates that a shop exists and returns it.
     */
    private AmazonShopDO validateShopExists(Long id) {
        AmazonShopDO shop = shopMapper.selectById(id);
        if (shop == null) {
            throw exception(SHOP_NOT_EXISTS);
        }
        return shop;
    }

    /**
     * Encrypts credential fields on the shop DO before persistence.
     */
    private void encryptCredentials(AmazonShopDO shop, String clientId,
                                     String clientSecret, String refreshToken) {
        if (clientId != null && !clientId.trim().isEmpty()) {
            shop.setClientId(credentialEncryptor.encrypt(clientId));
        }
        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            shop.setClientSecret(credentialEncryptor.encrypt(clientSecret));
        }
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            shop.setRefreshToken(credentialEncryptor.encrypt(refreshToken));
        }
    }

    /**
     * Builds the SP-API OAuth authorisation URL for a shop.
     *
     * <p>The URL follows the Login with Amazon (LWA) OAuth2 flow:
     * {@code https://sellercentral.amazon.com/apps/authorize/consent?application_id=xxx&state=xxx&...}
     */
    private String buildAuthorizeUrl(AmazonShopDO shop) {
        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(shop.getMarketplaceId());

        // Determine the Seller Central domain based on marketplace
        String sellerCentralDomain = resolveSellerCentralDomain(marketplace);

        // Build state: encode shop ID for callback identification
        String state = String.valueOf(shop.getId());

        // Construct the authorisation URL
        return "https://" + sellerCentralDomain + "/apps/authorize/consent"
                + "?application_id=" + urlEncode(amazonProperties.getAppId())
                + "&state=" + urlEncode(state)
                + "&redirect_uri=" + urlEncode(amazonProperties.getRedirectUri())
                + "&version=beta";
    }

    /**
     * Resolves the Seller Central domain for a marketplace.
     */
    private String resolveSellerCentralDomain(AmazonMarketplaceEnum marketplace) {
        if (marketplace == null) {
            return "sellercentral.amazon.com";
        }
        String region = marketplace.getRegion();
        if ("EU".equals(region)) {
            return "sellercentral-europe.amazon.com";
        } else if ("FE".equals(region)) {
            String cc = marketplace.getCountryCode();
            if ("JP".equals(cc)) {
                return "sellercentral.amazon.co.jp";
            } else if ("AU".equals(cc)) {
                return "sellercentral.amazon.com.au";
            } else if ("IN".equals(cc)) {
                return "sellercentral.amazon.in";
            } else if ("SG".equals(cc)) {
                return "sellercentral.amazon.sg";
            }
            return "sellercentral.amazon.com";
        }
        return "sellercentral.amazon.com";
    }

    private static String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
