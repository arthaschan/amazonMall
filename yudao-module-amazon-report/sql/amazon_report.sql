-- ----------------------------
-- Amazon Report 模块 DDL
-- ----------------------------

-- 仪表盘指标表
CREATE TABLE IF NOT EXISTS `amazon_dashboard_metric` (
  `id`                      bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`               bigint        NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`                 bigint        NOT NULL               COMMENT '关联店铺 ID',
  `metric_date`             date          NOT NULL               COMMENT '指标日期',
  `total_sales`             decimal(14,2) NULL     DEFAULT NULL   COMMENT '总销售额',
  `total_orders`            int           NULL     DEFAULT NULL   COMMENT '总订单数',
  `ad_spend`                decimal(12,2) NULL     DEFAULT NULL   COMMENT '广告花费',
  `profit`                  decimal(12,2) NULL     DEFAULT NULL   COMMENT '利润',
  `inventory_health_score`  decimal(6,2)  NULL     DEFAULT NULL   COMMENT '库存健康评分 0-100',
  `avg_rating`              decimal(3,2)  NULL     DEFAULT NULL   COMMENT '平均评分',
  `creator`                 varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`                 varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`                 bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_metric_date` (`metric_date`),
  UNIQUE INDEX `uk_shop_metric_date` (`shop_id`, `metric_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 仪表盘指标';

-- AI 周报
CREATE TABLE IF NOT EXISTS `amazon_weekly_report` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`           bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`             bigint       NOT NULL               COMMENT '关联店铺 ID',
  `report_week`         varchar(16)  NOT NULL DEFAULT ''     COMMENT '报告所属周 (ISO格式: 2024-W03)',
  `report_data`         json         NULL                     COMMENT '报告数据 JSON',
  `ai_summary`          text         NULL                     COMMENT 'AI 摘要',
  `ai_recommendations`  json         NULL                     COMMENT 'AI 建议列表 JSON',
  `status`              tinyint      NOT NULL DEFAULT 0      COMMENT '状态: 0=生成中, 1=已完成',
  `creator`             varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`             varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`             bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_report_week` (`report_week`),
  UNIQUE INDEX `uk_shop_week` (`shop_id`, `report_week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon AI 周报';
