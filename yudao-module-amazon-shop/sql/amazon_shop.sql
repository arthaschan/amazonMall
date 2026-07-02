-- --------------------------------------------------------
-- Amazon Shop Management Module (M1) - Database Schema
-- AmazonOps AI
-- --------------------------------------------------------

-- --------------------------------------------------------
-- Table: amazon_shop
-- Main shop/seller account table. Stores Amazon seller
-- credentials (encrypted), marketplace binding, and status.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `amazon_shop` (
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `tenant_id`        bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID (multi-tenant)',
    `shop_name`        varchar(100) NOT NULL DEFAULT ''     COMMENT 'Shop display name',
    `marketplace_id`   varchar(32)  NOT NULL DEFAULT ''     COMMENT 'Amazon marketplace ID (e.g. ATVPDKIKX0DER)',
    `country_code`     varchar(10)  NOT NULL DEFAULT ''     COMMENT 'ISO 3166-1 alpha-2 country code (e.g. US)',
    `seller_id`        varchar(64)  NOT NULL DEFAULT ''     COMMENT 'Amazon seller ID',
    `client_id`        text                                 COMMENT 'LWA client ID (encrypted AES-256-GCM)',
    `client_secret`    text                                 COMMENT 'LWA client secret (encrypted AES-256-GCM)',
    `refresh_token`    text                                 COMMENT 'SP-API refresh token (encrypted AES-256-GCM)',
    `access_token`     text                                 COMMENT 'SP-API access token (encrypted AES-256-GCM, cached)',
    `token_expire_at`  datetime     NULL     DEFAULT NULL   COMMENT 'Access token expiry time',
    `iam_arn`          varchar(256) NULL     DEFAULT NULL   COMMENT 'AWS IAM Role ARN for SP-API signing',
    `status`           tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0=disabled, 1=enabled, 2=auth_expired, 3=auth_pending',
    `creator`          varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator user ID',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updater`          varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater user ID',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    `deleted`          bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete flag',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_seller_id` (`seller_id`),
    INDEX `idx_marketplace_id` (`marketplace_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon shop/seller account';

-- --------------------------------------------------------
-- Table: amazon_marketplace
-- Marketplace configuration (reference data).
-- Stores endpoint URLs, currency codes, and regional info
-- for each Amazon marketplace.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `amazon_marketplace` (
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `marketplace_id`  varchar(32)  NOT NULL DEFAULT ''     COMMENT 'Amazon marketplace ID (e.g. ATVPDKIKX0DER)',
    `region`          varchar(16)  NOT NULL DEFAULT ''     COMMENT 'Region code (NA/EU/FE)',
    `country`         varchar(10)  NOT NULL DEFAULT ''     COMMENT 'ISO 3166-1 alpha-2 country code',
    `name`            varchar(64)  NOT NULL DEFAULT ''     COMMENT 'Marketplace display name',
    `currency_code`   varchar(10)  NOT NULL DEFAULT ''     COMMENT 'ISO 4217 currency code (e.g. USD)',
    `endpoint_url`    varchar(128) NOT NULL DEFAULT ''     COMMENT 'SP-API regional endpoint URL',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_marketplace_id` (`marketplace_id`),
    INDEX `idx_region` (`region`),
    INDEX `idx_country` (`country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon marketplace configuration';

-- --------------------------------------------------------
-- Table: amazon_api_log
-- API call audit log. Records every SP-API request for
-- monitoring, debugging, and rate-limit analysis.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `amazon_api_log` (
    `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `shop_id`               bigint       NOT NULL DEFAULT 0     COMMENT 'FK to amazon_shop.id',
    `tenant_id`             bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID',
    `api_endpoint`          varchar(256) NOT NULL DEFAULT ''     COMMENT 'API endpoint path (e.g. /orders/v0/orders)',
    `request_method`        varchar(10)  NOT NULL DEFAULT ''     COMMENT 'HTTP method (GET/POST/PUT/DELETE)',
    `request_params`        text                                 COMMENT 'Sanitized request parameters (no secrets)',
    `response_code`         int          NULL     DEFAULT NULL   COMMENT 'HTTP response status code',
    `response_time_ms`      int          NULL     DEFAULT NULL   COMMENT 'Response time in milliseconds',
    `rate_limit_remaining`  double       NULL     DEFAULT NULL   COMMENT 'x-amzn-RateLimit-Limit header value',
    `error_message`         text                                 COMMENT 'Error message if request failed',
    `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Request timestamp',
    PRIMARY KEY (`id`),
    INDEX `idx_shop_id` (`shop_id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_api_endpoint` (`api_endpoint`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_response_code` (`response_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon SP-API call audit log';

-- --------------------------------------------------------
-- Seed data: Amazon marketplace configurations
-- --------------------------------------------------------
INSERT INTO `amazon_marketplace` (`marketplace_id`, `region`, `country`, `name`, `currency_code`, `endpoint_url`) VALUES
('ATVPDKIKX0DER',  'NA', 'US', 'Amazon.com',           'USD', 'https://sellingpartnerapi-na.amazon.com'),
('A2IR4J4P5XJMGM', 'NA', 'CA', 'Amazon.ca',            'CAD', 'https://sellingpartnerapi-na.amazon.com'),
('A1AM78C64UM0Y8', 'NA', 'MX', 'Amazon.com.mx',        'MXN', 'https://sellingpartnerapi-na.amazon.com'),
('A1PA6795UKMFR9', 'EU', 'DE', 'Amazon.de',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A1RKKUPIHCS9HS', 'EU', 'ES', 'Amazon.es',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A13V1IB3VIYZZH', 'EU', 'FR', 'Amazon.fr',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('APJ6JRA9NG5V4',  'EU', 'IT', 'Amazon.it',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A1F83G8C2ARO7P', 'EU', 'UK', 'Amazon.co.uk',         'GBP', 'https://sellingpartnerapi-eu.amazon.com'),
('A1805IZSGTT6HS', 'EU', 'NL', 'Amazon.nl',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A2NODRKZP88ZB9', 'EU', 'SE', 'Amazon.se',            'SEK', 'https://sellingpartnerapi-eu.amazon.com'),
('A1C3SOZRARQ6R3', 'EU', 'PL', 'Amazon.pl',            'PLN', 'https://sellingpartnerapi-eu.amazon.com'),
('AMEN7S2L3V6F',   'EU', 'BE', 'Amazon.com.be',        'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A2Q3Y263D00KWC', 'EU', 'TR', 'Amazon.com.tr',        'TRY', 'https://sellingpartnerapi-eu.amazon.com'),
('ARBP9OOSHTCHU',  'EU', 'EG', 'Amazon.eg',            'EGP', 'https://sellingpartnerapi-eu.amazon.com'),
('A2VIGQ357CS4UG', 'EU', 'AE', 'Amazon.ae',            'AED', 'https://sellingpartnerapi-eu.amazon.com'),
('A17E79C6D8DWNP', 'EU', 'SA', 'Amazon.sa',            'SAR', 'https://sellingpartnerapi-eu.amazon.com'),
('A1VC38T7YXB528', 'FE', 'JP', 'Amazon.co.jp',         'JPY', 'https://sellingpartnerapi-fe.amazon.com'),
('A39IBJ37TRP1C6', 'FE', 'AU', 'Amazon.com.au',        'AUD', 'https://sellingpartnerapi-fe.amazon.com'),
('A21TJRUUN4KGV',  'FE', 'IN', 'Amazon.in',            'INR', 'https://sellingpartnerapi-fe.amazon.com'),
('A19VAU5U5O7RUS', 'FE', 'SG', 'Amazon.sg',            'SGD', 'https://sellingpartnerapi-fe.amazon.com');
