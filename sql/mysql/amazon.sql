-- =============================================
-- AmazonOps AI - MySQL DDL Scripts
-- All Amazon module tables
--
-- Target Server Type    : MySQL
-- Target Server Version : 80200 (8.2.0)
-- File Encoding         : 65001
-- Date: 02/07/2026
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ===========================================
-- Module: amazon-shop
-- ===========================================

-- ----------------------------
-- Table structure for amazon_shop
-- ----------------------------
DROP TABLE IF EXISTS `amazon_shop`;
CREATE TABLE `amazon_shop` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '店铺名称',
  `marketplace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Amazon 站点 ID',
  `country_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '国家代码 (ISO 3166-1 alpha-2)',
  `seller_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Amazon 卖家 ID',
  `client_id` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'LWA Client ID (AES-256-GCM 加密)',
  `client_secret` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'LWA Client Secret (加密)',
  `refresh_token` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'SP-API Refresh Token (加密)',
  `access_token` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'SP-API Access Token (加密)',
  `token_expire_at` datetime NULL DEFAULT NULL COMMENT 'Access Token 过期时间',
  `iam_arn` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'AWS IAM Role ARN',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0=禁用 1=启用 2=授权过期 3=授权中',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_marketplace_id`(`marketplace_id` ASC) USING BTREE,
  INDEX `idx_seller_id`(`seller_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon 店铺';

-- ----------------------------
-- Table structure for amazon_marketplace
-- ----------------------------
DROP TABLE IF EXISTS `amazon_marketplace`;
CREATE TABLE `amazon_marketplace` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `marketplace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Amazon 站点 ID',
  `region` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '区域: NA/EU/FE',
  `country` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '国家代码',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '站点名称',
  `currency_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '货币代码 (ISO 4217)',
  `endpoint_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'SP-API 端点 URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_marketplace_id`(`marketplace_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon 站点信息';

-- ----------------------------
-- Table structure for amazon_api_log
-- ----------------------------
DROP TABLE IF EXISTS `amazon_api_log`;
CREATE TABLE `amazon_api_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `api_endpoint` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'API 端点路径',
  `request_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'HTTP 方法',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求参数 (脱敏)',
  `response_code` int NULL DEFAULT NULL COMMENT 'HTTP 状态码',
  `response_time_ms` int NULL DEFAULT NULL COMMENT '响应耗时 (ms)',
  `rate_limit_remaining` double NULL DEFAULT NULL COMMENT '剩余限流配额',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_shop_id`(`shop_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon SP-API 调用日志';

-- ===========================================
-- Module: amazon-research
-- ===========================================

-- ----------------------------
-- Table structure for amazon_bsr_regression
-- ----------------------------
DROP TABLE IF EXISTS `amazon_bsr_regression`;
CREATE TABLE `amazon_bsr_regression` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '类目 ID',
  `marketplace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '站点 ID',
  `coefficient_a` decimal(20,8) NULL DEFAULT NULL COMMENT '回归系数 a',
  `coefficient_alpha` decimal(20,8) NULL DEFAULT NULL COMMENT '回归指数 alpha',
  `r_squared` decimal(10,6) NULL DEFAULT NULL COMMENT 'R 平方 (拟合优度)',
  `sample_size` int NULL DEFAULT NULL COMMENT '样本量',
  `last_fit_date` datetime NULL DEFAULT NULL COMMENT '最近拟合日期',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_marketplace`(`category_id` ASC, `marketplace_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'BSR-销量回归模型';

-- ----------------------------
-- Table structure for amazon_financial_projection
-- ----------------------------
DROP TABLE IF EXISTS `amazon_financial_projection`;
CREATE TABLE `amazon_financial_projection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `opportunity_id` bigint NOT NULL COMMENT '关联产品机会 ID',
  `startup_cost` decimal(14,2) NULL DEFAULT NULL COMMENT '启动成本',
  `break_even_week` int NULL DEFAULT NULL COMMENT '回本周数',
  `twelve_month_profit` decimal(14,2) NULL DEFAULT NULL COMMENT '12 个月累计利润',
  `brand_valuation` decimal(14,2) NULL DEFAULT NULL COMMENT '品牌估值',
  `projection_data` json NULL COMMENT '周度预测数据 JSON',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_opportunity_id`(`opportunity_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '财务预测';

-- ----------------------------
-- Table structure for amazon_niche
-- ----------------------------
DROP TABLE IF EXISTS `amazon_niche`;
CREATE TABLE `amazon_niche` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '细分品类名称',
  `marketplace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '站点 ID',
  `category` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '类目',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0=草稿 1=分析中 2=完成 3=归档',
  `omniscient_score` decimal(6,2) NULL DEFAULT NULL COMMENT '综合评分 (0-100)',
  `demand_score` decimal(6,2) NULL DEFAULT NULL COMMENT '需求维度',
  `competition_score` decimal(6,2) NULL DEFAULT NULL COMMENT '竞争维度',
  `profitability_score` decimal(6,2) NULL DEFAULT NULL COMMENT '盈利维度',
  `review_moat_score` decimal(6,2) NULL DEFAULT NULL COMMENT '评论壁垒',
  `price_stability_score` decimal(6,2) NULL DEFAULT NULL COMMENT '价格稳定性',
  `seasonality_score` decimal(6,2) NULL DEFAULT NULL COMMENT '季节性',
  `organic_rank_score` decimal(6,2) NULL DEFAULT NULL COMMENT '自然排名',
  `ad_dependency_score` decimal(6,2) NULL DEFAULT NULL COMMENT '广告依赖度',
  `supplier_score` decimal(6,2) NULL DEFAULT NULL COMMENT '供应商维度',
  `score_weights` json NULL COMMENT '评分权重 JSON',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_marketplace_id`(`marketplace_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '细分品类分析';

-- ----------------------------
-- Table structure for amazon_product_opportunity
-- ----------------------------
DROP TABLE IF EXISTS `amazon_product_opportunity`;
CREATE TABLE `amazon_product_opportunity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `niche_id` bigint NOT NULL COMMENT '关联 Niche ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `title` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '产品标题',
  `price` decimal(12,2) NULL DEFAULT NULL COMMENT '价格',
  `currency` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '货币',
  `rating` decimal(3,2) NULL DEFAULT NULL COMMENT '星级评分',
  `review_count` int NULL DEFAULT NULL COMMENT '评论数量',
  `bsr` int NULL DEFAULT NULL COMMENT 'BSR 排名',
  `monthly_search_volume` bigint NULL DEFAULT NULL COMMENT '月搜索量',
  `estimated_monthly_sales` int NULL DEFAULT NULL COMMENT '预估月销量',
  `profit_margin` decimal(8,4) NULL DEFAULT NULL COMMENT '利润率',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_niche_id`(`niche_id` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '产品机会';

-- ----------------------------
-- Table structure for amazon_supplier_match
-- ----------------------------
DROP TABLE IF EXISTS `amazon_supplier_match`;
CREATE TABLE `amazon_supplier_match` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `opportunity_id` bigint NOT NULL COMMENT '关联产品机会 ID',
  `supplier_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '供应商名称',
  `supplier_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '供应商 URL',
  `min_order_qty` int NULL DEFAULT NULL COMMENT '最小起订量',
  `unit_price` decimal(12,2) NULL DEFAULT NULL COMMENT '单价',
  `rating` decimal(3,2) NULL DEFAULT NULL COMMENT '供应商评分',
  `location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '所在地',
  `factory_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '工厂类型: FACTORY/TRADING_COMPANY',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_opportunity_id`(`opportunity_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '供应商匹配';

-- ===========================================
-- Module: amazon-listing
-- ===========================================

-- ----------------------------
-- Table structure for amazon_product
-- ----------------------------
DROP TABLE IF EXISTS `amazon_product`;
CREATE TABLE `amazon_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `sku` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'SKU',
  `marketplace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '站点 ID',
  `title` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '标题',
  `brand` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '品牌',
  `category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '类目 ID',
  `price` decimal(12,2) NULL DEFAULT NULL COMMENT '价格',
  `currency` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '货币',
  `main_image_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '主图 URL',
  `bullet_points` json NULL COMMENT '五点描述 JSON',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '产品描述',
  `backend_keywords` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '后台关键词',
  `listing_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'Listing 状态: ACTIVE/INACTIVE/SUPPRESSED',
  `bsr_rank` int NULL DEFAULT NULL COMMENT 'BSR 排名',
  `rating` decimal(3,2) NULL DEFAULT NULL COMMENT '评分',
  `review_count` int NULL DEFAULT NULL COMMENT '评论数',
  `ai_listing_score` decimal(6,2) NULL DEFAULT NULL COMMENT 'AI Listing 评分 (0-100)',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE,
  INDEX `idx_sku`(`sku` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon 产品';

-- ----------------------------
-- Table structure for amazon_listing_version
-- ----------------------------
DROP TABLE IF EXISTS `amazon_listing_version`;
CREATE TABLE `amazon_listing_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '产品 ID',
  `version_num` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `title` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '标题快照',
  `bullet_points` json NULL COMMENT '五点描述快照 JSON',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述快照',
  `backend_keywords` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '后台关键词快照',
  `ai_generated` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否 AI 生成',
  `ai_score` decimal(6,2) NULL DEFAULT NULL COMMENT 'AI 评分',
  `change_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '变更摘要',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人 ID',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Listing 版本历史';

-- ----------------------------
-- Table structure for amazon_auto_price_rule
-- ----------------------------
DROP TABLE IF EXISTS `amazon_auto_price_rule`;
CREATE TABLE `amazon_auto_price_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '产品 ID',
  `rule_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '规则类型: COMPETITIVE/PROFIT_BASED/BUY_BOX',
  `condition_json` json NULL COMMENT '触发条件 JSON',
  `action_json` json NULL COMMENT '执行动作 JSON',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0=禁用 1=启用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '自动定价规则';

-- ===========================================
-- Module: amazon-order
-- ===========================================

-- ----------------------------
-- Table structure for amazon_order
-- ----------------------------
DROP TABLE IF EXISTS `amazon_order`;
CREATE TABLE `amazon_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `amazon_order_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Amazon 订单号',
  `marketplace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '站点 ID',
  `order_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '订单状态',
  `order_total` decimal(12,2) NULL DEFAULT NULL COMMENT '订单总额',
  `currency` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '货币',
  `purchase_date` datetime NULL DEFAULT NULL COMMENT '下单时间',
  `fulfillment_channel` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '配送渠道: AFN/MFN',
  `is_business_order` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否企业订单',
  `is_prime` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否 Prime',
  `ship_city` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '收货城市',
  `ship_state` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '收货州/省',
  `ship_country` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '收货国家',
  `item_count` int NULL DEFAULT NULL COMMENT '商品数量',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_amazon_order_id`(`amazon_order_id` ASC) USING BTREE,
  INDEX `idx_purchase_date`(`purchase_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon 订单';

-- ----------------------------
-- Table structure for amazon_order_item
-- ----------------------------
DROP TABLE IF EXISTS `amazon_order_item`;
CREATE TABLE `amazon_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单 ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `sku` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'SKU',
  `title` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '商品标题',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `price` decimal(12,2) NULL DEFAULT NULL COMMENT '单价',
  `currency` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '货币',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon 订单明细';

-- ----------------------------
-- Table structure for amazon_fba_shipment
-- ----------------------------
DROP TABLE IF EXISTS `amazon_fba_shipment`;
CREATE TABLE `amazon_fba_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `shipment_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Amazon 货件 ID',
  `shipment_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '货件名称',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '状态: WORKING/SHIPPED/IN_TRANSIT/DELIVERED/CHECKED_IN/RECEIVING/CLOSED/CANCELLED',
  `destination_fc` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '目的地仓库代码',
  `ship_from_address` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '发货地址',
  `label_count` int NULL DEFAULT NULL COMMENT '标签数量',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_shipment_id`(`shipment_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'FBA 货件';

-- ===========================================
-- Module: amazon-inventory
-- ===========================================

-- ----------------------------
-- Table structure for amazon_inventory
-- ----------------------------
DROP TABLE IF EXISTS `amazon_inventory`;
CREATE TABLE `amazon_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `sku` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'SKU',
  `fulfillment_center` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓库代码',
  `available_qty` int NULL DEFAULT 0 COMMENT '可用数量',
  `reserved_qty` int NULL DEFAULT 0 COMMENT '预留数量',
  `inbound_qty` int NULL DEFAULT 0 COMMENT '在途数量',
  `unfulfillable_qty` int NULL DEFAULT 0 COMMENT '不可售数量',
  `days_of_supply` int NULL DEFAULT NULL COMMENT '可供天数',
  `snapshot_date` date NULL DEFAULT NULL COMMENT '快照日期',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE,
  INDEX `idx_snapshot_date`(`snapshot_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon 库存';

-- ----------------------------
-- Table structure for amazon_inventory_forecast
-- ----------------------------
DROP TABLE IF EXISTS `amazon_inventory_forecast`;
CREATE TABLE `amazon_inventory_forecast` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `forecast_date` date NOT NULL COMMENT '预测日期',
  `predicted_daily_sales` decimal(10,2) NULL DEFAULT NULL COMMENT '预测日销量',
  `confidence` decimal(5,4) NULL DEFAULT NULL COMMENT '置信度 (0-1)',
  `reorder_point` int NULL DEFAULT NULL COMMENT '补货点',
  `safety_stock` int NULL DEFAULT NULL COMMENT '安全库存',
  `suggested_reorder_qty` int NULL DEFAULT NULL COMMENT '建议补货量',
  `lead_time_days` int NULL DEFAULT NULL COMMENT '备货周期 (天)',
  `generate_date` date NULL DEFAULT NULL COMMENT '预测生成日期',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop_asin`(`tenant_id` ASC, `shop_id` ASC, `asin` ASC) USING BTREE,
  INDEX `idx_forecast_date`(`forecast_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '库存预测';

-- ----------------------------
-- Table structure for amazon_replenish_alert
-- ----------------------------
DROP TABLE IF EXISTS `amazon_replenish_alert`;
CREATE TABLE `amazon_replenish_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `alert_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '告警类型: LOW_STOCK/OUT_OF_STOCK/OVERSTOCK',
  `current_qty` int NULL DEFAULT NULL COMMENT '当前库存',
  `reorder_point` int NULL DEFAULT NULL COMMENT '补货点',
  `suggested_qty` int NULL DEFAULT NULL COMMENT '建议补货量',
  `acknowledged` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已确认',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '补货告警';

-- ===========================================
-- Module: amazon-ad
-- ===========================================

-- ----------------------------
-- Table structure for amazon_ad_campaign
-- ----------------------------
DROP TABLE IF EXISTS `amazon_ad_campaign`;
CREATE TABLE `amazon_ad_campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `campaign_id` bigint NOT NULL COMMENT 'Amazon 广告活动 ID',
  `campaign_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '广告活动名称',
  `campaign_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '广告类型: SP/SB/SD',
  `targeting_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '投放类型: MANUAL/AUTO',
  `daily_budget` decimal(12,2) NULL DEFAULT NULL COMMENT '日预算',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '状态: enabled/paused/archived',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_campaign_id`(`campaign_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '广告活动';

-- ----------------------------
-- Table structure for amazon_ad_report_daily
-- ----------------------------
DROP TABLE IF EXISTS `amazon_ad_report_daily`;
CREATE TABLE `amazon_ad_report_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `campaign_id` bigint NOT NULL COMMENT '广告活动 ID',
  `ad_group_id` bigint NULL DEFAULT NULL COMMENT '广告组 ID',
  `keyword_id` bigint NULL DEFAULT NULL COMMENT '关键词 ID',
  `keyword_text` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '关键词文本',
  `match_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '匹配类型: EXACT/PHRASE/BROAD',
  `report_date` date NOT NULL COMMENT '报告日期',
  `impressions` bigint NULL DEFAULT 0 COMMENT '曝光量',
  `clicks` bigint NULL DEFAULT 0 COMMENT '点击量',
  `cost` decimal(12,4) NULL DEFAULT 0 COMMENT '花费',
  `sales` decimal(14,2) NULL DEFAULT 0 COMMENT '销售额',
  `orders` int NULL DEFAULT 0 COMMENT '订单数',
  `acos` decimal(8,4) NULL DEFAULT NULL COMMENT 'ACoS',
  `roas` decimal(8,4) NULL DEFAULT NULL COMMENT 'ROAS',
  `cpc` decimal(8,4) NULL DEFAULT NULL COMMENT 'CPC',
  `ctr` decimal(8,6) NULL DEFAULT NULL COMMENT 'CTR',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_campaign_date`(`campaign_id` ASC, `report_date` ASC) USING BTREE,
  INDEX `idx_report_date`(`report_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '广告日报';

-- ----------------------------
-- Table structure for amazon_ad_rule
-- ----------------------------
DROP TABLE IF EXISTS `amazon_ad_rule`;
CREATE TABLE `amazon_ad_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `rule_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '规则名称',
  `scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '作用范围: CAMPAIGN/ADGROUP/KEYWORD',
  `condition_json` json NULL COMMENT '触发条件 JSON',
  `action_json` json NULL COMMENT '执行动作 JSON',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0=禁用 1=启用',
  `last_executed_at` datetime NULL DEFAULT NULL COMMENT '最近执行时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '广告自动化规则';

-- ----------------------------
-- Table structure for amazon_ad_search_term
-- ----------------------------
DROP TABLE IF EXISTS `amazon_ad_search_term`;
CREATE TABLE `amazon_ad_search_term` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `campaign_id` bigint NOT NULL COMMENT '广告活动 ID',
  `search_term` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '用户搜索词',
  `report_date` date NOT NULL COMMENT '报告日期',
  `impressions` bigint NULL DEFAULT 0 COMMENT '曝光量',
  `clicks` bigint NULL DEFAULT 0 COMMENT '点击量',
  `cost` decimal(12,4) NULL DEFAULT 0 COMMENT '花费',
  `sales` decimal(14,2) NULL DEFAULT 0 COMMENT '销售额',
  `orders` int NULL DEFAULT 0 COMMENT '订单数',
  `ai_tag` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'AI 标签: OPPORTUNITY/WASTE/KEEP/NEGATIVE',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_campaign_date`(`campaign_id` ASC, `report_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '广告搜索词报告';

-- ===========================================
-- Module: amazon-review
-- ===========================================

-- ----------------------------
-- Table structure for amazon_review
-- ----------------------------
DROP TABLE IF EXISTS `amazon_review`;
CREATE TABLE `amazon_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `review_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Amazon 评论 ID',
  `reviewer_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '评论者名称',
  `rating` tinyint NOT NULL DEFAULT 0 COMMENT '星级 (1-5)',
  `title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '评论标题',
  `body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评论内容',
  `review_date` datetime NULL DEFAULT NULL COMMENT '评论时间',
  `verified_purchase` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否 VP 购买',
  `helpful_votes` int NULL DEFAULT 0 COMMENT '有用投票数',
  `ai_sentiment` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'AI 情感: POSITIVE/NEUTRAL/NEGATIVE',
  `ai_topics` json NULL COMMENT 'AI 话题标签 JSON',
  `ai_pain_points` json NULL COMMENT 'AI 痛点 JSON',
  `ai_selling_points` json NULL COMMENT 'AI 卖点 JSON',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE,
  UNIQUE INDEX `uk_review_id`(`review_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon 评论';

-- ----------------------------
-- Table structure for amazon_review_alert
-- ----------------------------
DROP TABLE IF EXISTS `amazon_review_alert`;
CREATE TABLE `amazon_review_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `asin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'ASIN',
  `review_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '评论 ID',
  `rating` tinyint NOT NULL DEFAULT 0 COMMENT '星级',
  `alert_time` datetime NULL DEFAULT NULL COMMENT '告警时间',
  `acknowledged` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已处理',
  `ai_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI 分析结果',
  `suggested_action` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '建议操作',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论告警';

-- ----------------------------
-- Table structure for amazon_customer_template
-- ----------------------------
DROP TABLE IF EXISTS `amazon_customer_template`;
CREATE TABLE `amazon_customer_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `template_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '模板名称',
  `template_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '模板类型: REFUND/EXCHANGE/GUIDE/THANKS/APOLOGY',
  `language` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en' COMMENT '语言代码',
  `subject` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '邮件主题',
  `body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '邮件正文',
  `variables` json NULL COMMENT '变量列表 JSON',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '客服消息模板';

-- ===========================================
-- Module: amazon-report
-- ===========================================

-- ----------------------------
-- Table structure for amazon_dashboard_metric
-- ----------------------------
DROP TABLE IF EXISTS `amazon_dashboard_metric`;
CREATE TABLE `amazon_dashboard_metric` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `metric_date` date NOT NULL COMMENT '指标日期',
  `total_sales` decimal(14,2) NULL DEFAULT NULL COMMENT '总销售额',
  `total_orders` int NULL DEFAULT NULL COMMENT '总订单数',
  `ad_spend` decimal(12,2) NULL DEFAULT NULL COMMENT '广告花费',
  `profit` decimal(14,2) NULL DEFAULT NULL COMMENT '利润',
  `inventory_health_score` decimal(6,2) NULL DEFAULT NULL COMMENT '库存健康评分 (0-100)',
  `avg_rating` decimal(3,2) NULL DEFAULT NULL COMMENT '平均评分',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_metric_date`(`metric_date` ASC) USING BTREE,
  UNIQUE INDEX `uk_shop_date`(`shop_id` ASC, `metric_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '仪表盘每日指标';

-- ----------------------------
-- Table structure for amazon_weekly_report
-- ----------------------------
DROP TABLE IF EXISTS `amazon_weekly_report`;
CREATE TABLE `amazon_weekly_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `shop_id` bigint NOT NULL COMMENT '店铺 ID',
  `report_week` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '报告周 (ISO: 2024-W03)',
  `report_data` json NULL COMMENT '报告数据 JSON',
  `ai_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI 摘要',
  `ai_recommendations` json NULL COMMENT 'AI 建议列表 JSON',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0=生成中 1=已完成',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_shop`(`tenant_id` ASC, `shop_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_shop_week`(`shop_id` ASC, `report_week` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI 周报';

-- ===========================================
-- Module: amazon-ai-core
-- ===========================================

-- ----------------------------
-- Table structure for amazon_ai_token_usage
-- ----------------------------
DROP TABLE IF EXISTS `amazon_ai_token_usage`;
CREATE TABLE `amazon_ai_token_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `task_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'AI 任务类型',
  `model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '模型名称',
  `input_tokens` int NOT NULL DEFAULT 0 COMMENT '输入 Token 数',
  `output_tokens` int NOT NULL DEFAULT 0 COMMENT '输出 Token 数',
  `total_tokens` int NOT NULL DEFAULT 0 COMMENT '总 Token 数',
  `estimated_cost_usd` double NULL DEFAULT NULL COMMENT '预估费用 (USD)',
  `business_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '业务实体 ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户 ID',
  `year_month` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '年月 (如 2025-06)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_year_month`(`year_month` ASC) USING BTREE,
  INDEX `idx_task_type`(`task_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI Token 用量统计';

-- ===========================================
-- Initial Data: amazon_marketplace
-- ===========================================
BEGIN;
INSERT INTO `amazon_marketplace` (`marketplace_id`, `region`, `country`, `name`, `currency_code`, `endpoint_url`) VALUES
('ATVPDKIKX0DER',  'NA', 'US', 'Amazon.com',           'USD', 'https://sellingpartnerapi-na.amazon.com'),
('A2EUQ1WTGCTBG2', 'NA', 'CA', 'Amazon.ca',            'CAD', 'https://sellingpartnerapi-na.amazon.com'),
('A1AM78C64UM0Y8', 'NA', 'MX', 'Amazon.com.mx',        'MXN', 'https://sellingpartnerapi-na.amazon.com'),
('A2Q3Y263D00KWC', 'NA', 'BR', 'Amazon.com.br',        'BRL', 'https://sellingpartnerapi-na.amazon.com'),
('A1PA6795UKMFR9', 'EU', 'DE', 'Amazon.de',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A1RKKUPIHCS9HS', 'EU', 'ES', 'Amazon.es',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A13V1IB3VIYZZH', 'EU', 'FR', 'Amazon.fr',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('APJ6JRA9NG5V4',  'EU', 'IT', 'Amazon.it',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A1F83G8C2ARO7P', 'EU', 'GB', 'Amazon.co.uk',         'GBP', 'https://sellingpartnerapi-eu.amazon.com'),
('A1805IZSGTT6HS', 'EU', 'NL', 'Amazon.nl',            'EUR', 'https://sellingpartnerapi-eu.amazon.com'),
('A2NODRKZP88ZB9', 'EU', 'SE', 'Amazon.se',            'SEK', 'https://sellingpartnerapi-eu.amazon.com'),
('A33AVAJ2PDY3EV', 'EU', 'TR', 'Amazon.com.tr',        'TRY', 'https://sellingpartnerapi-eu.amazon.com'),
('ARBP9OOSHTCHU',  'EU', 'EG', 'Amazon.eg',            'EGP', 'https://sellingpartnerapi-eu.amazon.com'),
('A1VC38T7YXB528', 'FE', 'JP', 'Amazon.co.jp',         'JPY', 'https://sellingpartnerapi-fe.amazon.com'),
('A39IBJ37TRP1C6', 'FE', 'AU', 'Amazon.com.au',        'AUD', 'https://sellingpartnerapi-fe.amazon.com'),
('A21TJRUUN4KGV',  'FE', 'IN', 'Amazon.in',            'INR', 'https://sellingpartnerapi-fe.amazon.com'),
('A19VAU5U5O7RUS', 'FE', 'SG', 'Amazon.sg',            'SGD', 'https://sellingpartnerapi-fe.amazon.com'),
('A2VIGQ35RCS4UG', 'FE', 'AE', 'Amazon.ae',            'AED', 'https://sellingpartnerapi-fe.amazon.com'),
('A17E79C6D8DWNP', 'FE', 'SA', 'Amazon.sa',            'SAR', 'https://sellingpartnerapi-fe.amazon.com');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
