-- ----------------------------
-- Amazon Listing 模块 DDL
-- ----------------------------

-- 产品 Listing 表
CREATE TABLE IF NOT EXISTS `amazon_product` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`         bigint        NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`           bigint        NOT NULL               COMMENT '关联店铺 ID',
  `asin`              varchar(16)   NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `sku`               varchar(128)  NULL     DEFAULT ''     COMMENT 'SKU',
  `marketplace_id`    varchar(32)   NOT NULL DEFAULT ''     COMMENT 'Amazon Marketplace ID',
  `title`             varchar(512)  NULL     DEFAULT ''     COMMENT '商品标题',
  `brand`             varchar(128)  NULL     DEFAULT ''     COMMENT '品牌',
  `category_id`       varchar(128)  NULL     DEFAULT ''     COMMENT '类目 ID',
  `price`             decimal(10,2) NULL     DEFAULT NULL   COMMENT '售价',
  `currency`          varchar(8)    NOT NULL DEFAULT 'USD'  COMMENT '币种',
  `main_image_url`    varchar(1024) NULL     DEFAULT ''     COMMENT '主图 URL',
  `bullet_points`     json          NULL                     COMMENT '五点描述 JSON',
  `description`       text          NULL                     COMMENT '商品描述',
  `backend_keywords`  varchar(1024) NULL     DEFAULT ''     COMMENT '后台关键词',
  `listing_status`    varchar(16)   NULL     DEFAULT ''     COMMENT 'Listing状态: ACTIVE / INACTIVE / SUPPRESSED',
  `bsr_rank`          int           NULL     DEFAULT NULL   COMMENT 'BSR 排名',
  `rating`            decimal(3,2)  NULL     DEFAULT NULL   COMMENT '星级评分',
  `review_count`      int           NULL     DEFAULT NULL   COMMENT '评论数',
  `ai_listing_score`  decimal(6,2)  NULL     DEFAULT NULL   COMMENT 'AI Listing 评分 0-100',
  `creator`           varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`           varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`           bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_asin` (`asin`),
  INDEX `idx_sku` (`sku`),
  INDEX `idx_marketplace_id` (`marketplace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 产品 Listing';

-- Listing 版本记录表
CREATE TABLE IF NOT EXISTS `amazon_listing_version` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `product_id`      bigint        NOT NULL               COMMENT '关联产品 ID',
  `version_num`     int           NOT NULL DEFAULT 1      COMMENT '版本号',
  `title`           varchar(512)  NULL     DEFAULT ''     COMMENT '标题',
  `bullet_points`   json          NULL                     COMMENT '五点描述 JSON',
  `description`     text          NULL                     COMMENT '商品描述',
  `backend_keywords` varchar(1024) NULL    DEFAULT ''     COMMENT '后台关键词',
  `ai_generated`    bit(1)        NOT NULL DEFAULT b'0'   COMMENT '是否 AI 生成',
  `ai_score`        decimal(6,2)  NULL     DEFAULT NULL   COMMENT 'AI 评分',
  `change_summary`  varchar(512)  NULL     DEFAULT ''     COMMENT '变更摘要',
  `operator_id`     bigint        NULL     DEFAULT NULL   COMMENT '操作人 ID',
  `creator`         varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`         varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`         bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_product_id` (`product_id`),
  INDEX `idx_version_num` (`version_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon Listing 版本记录';

-- 自动调价规则表
CREATE TABLE IF NOT EXISTS `amazon_auto_price_rule` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `product_id`      bigint       NOT NULL               COMMENT '关联产品 ID',
  `rule_type`       varchar(32)  NOT NULL DEFAULT ''     COMMENT '规则类型: COMPETITIVE / PROFIT_BASED / BUY_BOX',
  `condition_json`  json         NULL                     COMMENT '条件 JSON',
  `action_json`     json         NULL                     COMMENT '动作 JSON',
  `status`          tinyint      NOT NULL DEFAULT 1      COMMENT '状态: 0=禁用, 1=启用',
  `creator`         varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`         varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`         bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_product_id` (`product_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 自动调价规则';
