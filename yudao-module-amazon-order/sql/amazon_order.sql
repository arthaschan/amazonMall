-- ----------------------------
-- Amazon Order 模块 DDL
-- ----------------------------

-- 订单表
CREATE TABLE IF NOT EXISTS `amazon_order` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`           bigint        NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`             bigint        NOT NULL               COMMENT '关联店铺 ID',
  `amazon_order_id`     varchar(32)   NOT NULL DEFAULT ''     COMMENT 'Amazon 订单号',
  `marketplace_id`      varchar(32)   NOT NULL DEFAULT ''     COMMENT 'Amazon Marketplace ID',
  `order_status`        varchar(32)   NOT NULL DEFAULT ''     COMMENT '订单状态: Pending/Unshipped/PartiallyShipped/Shipped/Canceled/Unfulfillable',
  `order_total`         decimal(12,2) NULL     DEFAULT NULL   COMMENT '订单总金额',
  `currency`            varchar(8)    NOT NULL DEFAULT 'USD'  COMMENT '币种',
  `purchase_date`       datetime      NULL     DEFAULT NULL   COMMENT '下单时间',
  `fulfillment_channel` varchar(8)    NULL     DEFAULT ''     COMMENT '配送渠道: AFN / MFN',
  `is_business_order`   bit(1)        NOT NULL DEFAULT b'0'   COMMENT '是否企业订单',
  `is_prime`            bit(1)        NOT NULL DEFAULT b'0'   COMMENT '是否 Prime',
  `ship_city`           varchar(128)  NULL     DEFAULT ''     COMMENT '配送城市',
  `ship_state`          varchar(64)   NULL     DEFAULT ''     COMMENT '配送州/省',
  `ship_country`        varchar(8)    NULL     DEFAULT ''     COMMENT '配送国家',
  `item_count`          int           NULL     DEFAULT NULL   COMMENT '商品数量',
  `creator`             varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`             varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`             bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  UNIQUE INDEX `uk_amazon_order_id` (`amazon_order_id`),
  INDEX `idx_marketplace_id` (`marketplace_id`),
  INDEX `idx_order_status` (`order_status`),
  INDEX `idx_purchase_date` (`purchase_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 订单';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `amazon_order_item` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `order_id`    bigint        NOT NULL               COMMENT '关联订单 ID',
  `asin`        varchar(16)   NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `sku`         varchar(128)  NULL     DEFAULT ''     COMMENT 'SKU',
  `title`       varchar(512)  NULL     DEFAULT ''     COMMENT '商品标题',
  `quantity`    int           NOT NULL DEFAULT 1      COMMENT '数量',
  `price`       decimal(10,2) NULL     DEFAULT NULL   COMMENT '单价',
  `currency`    varchar(8)    NOT NULL DEFAULT 'USD'  COMMENT '币种',
  `creator`     varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`     varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`     bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_asin` (`asin`),
  INDEX `idx_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 订单明细';

-- FBA 货件表
CREATE TABLE IF NOT EXISTS `amazon_fba_shipment` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`         bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`           bigint       NOT NULL               COMMENT '关联店铺 ID',
  `shipment_id`       varchar(32)  NOT NULL DEFAULT ''     COMMENT 'Amazon 货件 ID',
  `shipment_name`     varchar(255) NULL     DEFAULT ''     COMMENT '货件名称',
  `status`            varchar(32)  NOT NULL DEFAULT ''     COMMENT '状态: WORKING/SHIPPED/IN_TRANSIT/DELIVERED/CHECKED_IN/RECEIVING/CLOSED/CANCELLED',
  `destination_fc`    varchar(16)  NULL     DEFAULT ''     COMMENT '目的仓库代码',
  `ship_from_address` varchar(512) NULL     DEFAULT ''     COMMENT '发货地址',
  `label_count`       int          NULL     DEFAULT NULL   COMMENT '标签数量',
  `creator`           varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`           varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  UNIQUE INDEX `uk_shipment_id` (`shipment_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon FBA 货件';
