-- ----------------------------
-- Amazon Inventory 模块 DDL
-- ----------------------------

-- FBA 库存快照表
CREATE TABLE IF NOT EXISTS `amazon_inventory` (
  `id`                  bigint      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`           bigint      NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`             bigint      NOT NULL               COMMENT '关联店铺 ID',
  `asin`                varchar(16) NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `sku`                 varchar(128) NULL    DEFAULT ''     COMMENT 'SKU',
  `fulfillment_center`  varchar(16) NULL    DEFAULT ''     COMMENT '仓库代码',
  `available_qty`       int         NOT NULL DEFAULT 0      COMMENT '可售数量',
  `reserved_qty`        int         NOT NULL DEFAULT 0      COMMENT '预留数量',
  `inbound_qty`         int         NOT NULL DEFAULT 0      COMMENT '入库中数量',
  `unfulfillable_qty`   int         NOT NULL DEFAULT 0      COMMENT '不可售数量',
  `days_of_supply`      int         NULL     DEFAULT NULL   COMMENT '可售天数',
  `snapshot_date`       date        NOT NULL               COMMENT '快照日期',
  `creator`             varchar(64) NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`             varchar(64) NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`             bit(1)      NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_asin` (`asin`),
  INDEX `idx_sku` (`sku`),
  INDEX `idx_snapshot_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon FBA 库存快照';

-- 库存预测表
CREATE TABLE IF NOT EXISTS `amazon_inventory_forecast` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`             bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`               bigint       NOT NULL               COMMENT '关联店铺 ID',
  `asin`                  varchar(16)  NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `forecast_date`         date         NOT NULL               COMMENT '预测日期',
  `predicted_daily_sales` decimal(10,2) NULL   DEFAULT NULL   COMMENT '预测日均销量',
  `confidence`            decimal(4,2) NULL     DEFAULT NULL   COMMENT '置信度 0-1',
  `reorder_point`         int          NULL     DEFAULT NULL   COMMENT '再订购点',
  `safety_stock`          int          NULL     DEFAULT NULL   COMMENT '安全库存',
  `suggested_reorder_qty` int          NULL     DEFAULT NULL   COMMENT '建议补货量',
  `lead_time_days`        int          NULL     DEFAULT NULL   COMMENT '备货周期(天)',
  `generate_date`         date         NULL     DEFAULT NULL   COMMENT '预测生成日期',
  `creator`               varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`               varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`               bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_asin` (`asin`),
  INDEX `idx_forecast_date` (`forecast_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 库存预测';

-- 补货预警表
CREATE TABLE IF NOT EXISTS `amazon_replenish_alert` (
  `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`       bigint      NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`         bigint      NOT NULL               COMMENT '关联店铺 ID',
  `asin`            varchar(16) NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `alert_type`      varchar(32) NOT NULL DEFAULT ''     COMMENT '预警类型: LOW_STOCK / OUT_OF_STOCK / OVERSTOCK',
  `current_qty`     int         NOT NULL DEFAULT 0      COMMENT '当前库存',
  `reorder_point`   int         NULL     DEFAULT NULL   COMMENT '再订购点',
  `suggested_qty`   int         NULL     DEFAULT NULL   COMMENT '建议补货量',
  `acknowledged`    bit(1)      NOT NULL DEFAULT b'0'   COMMENT '是否已确认',
  `creator`         varchar(64) NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`         varchar(64) NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`         bit(1)      NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_asin` (`asin`),
  INDEX `idx_alert_type` (`alert_type`),
  INDEX `idx_acknowledged` (`acknowledged`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 补货预警';
