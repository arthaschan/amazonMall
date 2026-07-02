package cn.iocoder.yudao.module.amazon.shop.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Amazon Shop module error code constants.
 *
 * <p>Amazon Shop module uses the 1-010-000-000 segment.
 *
 * @author AmazonOps AI
 */
public interface ErrorCodeConstants {

    // ========== Shop CRUD 1-010-000-000 ==========
    ErrorCode SHOP_NOT_EXISTS = new ErrorCode(1_010_000_000, "Amazon shop does not exist");
    ErrorCode SHOP_MARKETPLACE_INVALID = new ErrorCode(1_010_000_001, "Invalid Amazon marketplace ID");
    ErrorCode SHOP_SELLER_MARKETPLACE_DUPLICATE = new ErrorCode(1_010_000_002,
            "A shop with this seller ID and marketplace already exists");
    ErrorCode SHOP_NOT_ENABLED = new ErrorCode(1_010_000_003,
            "Shop is not enabled; enable it before performing this operation");

    // ========== OAuth / Auth 1-010-001-000 ==========
    ErrorCode SHOP_AUTH_CALLBACK_INVALID_STATE = new ErrorCode(1_010_001_000,
            "Invalid OAuth callback state parameter");
    ErrorCode SHOP_AUTH_TOKEN_EXCHANGE_FAILED = new ErrorCode(1_010_001_001,
            "Failed to exchange OAuth authorisation code for tokens");
    ErrorCode SHOP_REFRESH_TOKEN_MISSING = new ErrorCode(1_010_001_002,
            "No refresh token configured for this shop; authorise first");
    ErrorCode SHOP_TOKEN_REFRESH_FAILED = new ErrorCode(1_010_001_003,
            "Token refresh failed; re-authorisation may be required");

    // ========== Data Sync 1-010-002-000 ==========
    ErrorCode SHOP_SYNC_ALREADY_RUNNING = new ErrorCode(1_010_002_000,
            "A data sync is already running for this shop");
}
