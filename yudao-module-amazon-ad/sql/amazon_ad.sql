-- ----------------------------
-- Amazon Ad 模块 DDL
-- ----------------------------

-- 广告活动表
CREATE TABLE IF NOT EXISTS `amazon_ad_campaign` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`       bigint        NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`         bigint        NOT NULL               COMMENT '关联店铺 ID',
  `campaign_id`     bigint        NOT NULL               COMMENT 'Amazon 广告活动 ID',
  `campaign_name`   varchar(255)  NULL     DEFAULT ''     COMMENT '广告活动名称',
  `campaign_type`   varchar(8)    NOT NULL DEFAULT ''     COMMENT '广告类型: SP / SB / SD',
  `targeting_type`  varchar(16)   NULL     DEFAULT ''     COMMENT '投放类型: MANUAL / AUTO',
  `daily_budget`    decimal(10,2) NULL     DEFAULT NULL   COMMENT '每日预算',
  `status`          varchar(16)   NOT NULL DEFAULT ''     COMMENT '状态: enabled / paused / archived',
  `start_date`      date          NULL     DEFAULT NULL   COMMENT '开始日期',
  `end_date`        date          NULL     DEFAULT NULL   COMMENT '结束日期',
  `creator`         varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`         varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`         bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  UNIQUE INDEX `uk_campaign_id` (`campaign_id`),
  INDEX `idx_campaign_type` (`campaign_type`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 广告活动';

-- 广告日报表
CREATE TABLE IF NOT EXISTS `amazon_ad_report_daily` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`     bigint        NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`       bigint        NOT NULL               COMMENT '关联店铺 ID',
  `campaign_id`   bigint        NOT NULL               COMMENT '广告活动 ID',
  `ad_group_id`   bigint        NULL     DEFAULT NULL   COMMENT '广告组 ID',
  `keyword_id`    bigint        NULL     DEFAULT NULL   COMMENT '关键词 ID',
  `keyword_text`  varchar(255)  NULL     DEFAULT ''     COMMENT '关键词文本',
  `match_type`    varchar(16)   NULL     DEFAULT ''     COMMENT '匹配类型: EXACT / PHRASE / BROAD',
  `report_date`   date          NOT NULL               COMMENT '报告日期',
  `impressions`   bigint        NOT NULL DEFAULT 0      COMMENT '曝光量',
  `clicks`        bigint        NOT NULL DEFAULT 0      COMMENT '点击量',
  `cost`          decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '花费',
  `sales`         decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '销售额',
  `orders`        int           NOT NULL DEFAULT 0      COMMENT '订单数',
  `acos`          decimal(8,4)  NULL     DEFAULT NULL   COMMENT 'ACoS',
  `roas`          decimal(8,4)  NULL     DEFAULT NULL   COMMENT 'ROAS',
  `cpc`           decimal(8,4)  NULL     DEFAULT NULL   COMMENT 'CPC',
  `ctr`           decimal(8,4)  NULL     DEFAULT NULL   COMMENT 'CTR',
  `creator`       varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`       varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_campaign_id` (`campaign_id`),
  INDEX `idx_report_date` (`report_date`),
  INDEX `idx_keyword_text` (`keyword_text`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 广告日报';

-- 广告搜索词报告表
CREATE TABLE IF NOT EXISTS `amazon_ad_search_term` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`     bigint        NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`       bigint        NOT NULL               COMMENT '关联店铺 ID',
  `campaign_id`   bigint        NOT NULL               COMMENT '广告活动 ID',
  `search_term`   varchar(255)  NOT NULL DEFAULT ''     COMMENT '用户实际搜索词',
  `report_date`   date          NOT NULL               COMMENT '报告日期',
  `impressions`   bigint        NOT NULL DEFAULT 0      COMMENT '曝光量',
  `clicks`        bigint        NOT NULL DEFAULT 0      COMMENT '点击量',
  `cost`          decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '花费',
  `sales`         decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '销售额',
  `orders`        int           NOT NULL DEFAULT 0      COMMENT '订单数',
  `ai_tag`        varchar(16)   NULL     DEFAULT ''     COMMENT 'AI 标签: OPPORTUNITY / WASTE / KEEP / NEGATIVE',
  `creator`       varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`       varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_campaign_id` (`campaign_id`),
  INDEX `idx_search_term` (`search_term`),
  INDEX `idx_report_date` (`report_date`),
  INDEX `idx_ai_tag` (`ai_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 广告搜索词报告';

-- 广告自动化规则表
CREATE TABLE IF NOT EXISTS `amazon_ad_rule` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`       bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`         bigint       NOT NULL               COMMENT '关联店铺 ID',
  `rule_name`       varchar(128) NOT NULL DEFAULT ''     COMMENT '规则名称',
  `scope`           varchar(16)  NOT NULL DEFAULT ''     COMMENT '作用范围: CAMPAIGN / ADGROUP / KEYWORD',
  `condition_json`  json         NULL                     COMMENT '条件 JSON',
  `action_json`     json         NULL                     COMMENT '动作 JSON',
  `status`          tinyint      NOT NULL DEFAULT 1      COMMENT '状态: 0=禁用, 1=启用',
  `last_executed_at` datetime    NULL     DEFAULT NULL   COMMENT '最后执行时间',
  `creator`         varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`         varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`         bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 广告自动化规则';
