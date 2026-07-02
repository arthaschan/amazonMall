-- ----------------------------
-- Amazon Review 模块 DDL
-- ----------------------------

-- 评论表
CREATE TABLE IF NOT EXISTS `amazon_review` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`         bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`           bigint       NOT NULL               COMMENT '关联店铺 ID',
  `asin`              varchar(16)  NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `review_id`         varchar(32)  NOT NULL DEFAULT ''     COMMENT 'Amazon 评论 ID',
  `reviewer_name`     varchar(128) NULL     DEFAULT ''     COMMENT '评论者名称',
  `rating`            tinyint      NOT NULL DEFAULT 0      COMMENT '星级 1-5',
  `title`             varchar(255) NULL     DEFAULT ''     COMMENT '评论标题',
  `body`              text         NULL                     COMMENT '评论正文',
  `review_date`       datetime     NULL     DEFAULT NULL   COMMENT '评论日期',
  `verified_purchase` bit(1)       NOT NULL DEFAULT b'0'   COMMENT '是否已验证购买',
  `helpful_votes`     int          NULL     DEFAULT NULL   COMMENT '有用票数',
  `ai_sentiment`      varchar(16)  NULL     DEFAULT ''     COMMENT 'AI 情感分析: POSITIVE / NEUTRAL / NEGATIVE',
  `ai_topics`         json         NULL                     COMMENT 'AI 提取的主题标签 JSON',
  `ai_pain_points`    json         NULL                     COMMENT 'AI 提取的痛点 JSON',
  `ai_selling_points` json         NULL                     COMMENT 'AI 提取的卖点 JSON',
  `creator`           varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`           varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  UNIQUE INDEX `uk_review_id` (`review_id`),
  INDEX `idx_asin` (`asin`),
  INDEX `idx_rating` (`rating`),
  INDEX `idx_ai_sentiment` (`ai_sentiment`),
  INDEX `idx_review_date` (`review_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 评论';

-- 评论预警表
CREATE TABLE IF NOT EXISTS `amazon_review_alert` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`        bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `shop_id`          bigint       NOT NULL               COMMENT '关联店铺 ID',
  `asin`             varchar(16)  NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `review_id`        varchar(32)  NOT NULL DEFAULT ''     COMMENT 'Amazon 评论 ID',
  `rating`           tinyint      NOT NULL DEFAULT 0      COMMENT '星级',
  `alert_time`       datetime     NOT NULL               COMMENT '预警时间',
  `acknowledged`     bit(1)       NOT NULL DEFAULT b'0'   COMMENT '是否已处理',
  `ai_analysis`      text         NULL                     COMMENT 'AI 分析结论',
  `suggested_action` varchar(512) NULL     DEFAULT ''     COMMENT '建议操作',
  `creator`          varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`          varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`          bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_shop_id` (`shop_id`),
  INDEX `idx_asin` (`asin`),
  INDEX `idx_acknowledged` (`acknowledged`),
  INDEX `idx_alert_time` (`alert_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 评论预警';

-- 客服模板表
CREATE TABLE IF NOT EXISTS `amazon_customer_template` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`       bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `template_name`   varchar(128) NOT NULL DEFAULT ''     COMMENT '模板名称',
  `template_type`   varchar(16)  NOT NULL DEFAULT ''     COMMENT '模板类型: REFUND / EXCHANGE / GUIDE / THANKS / APOLOGY',
  `language`        varchar(8)   NOT NULL DEFAULT 'en'   COMMENT '语言: en / zh / de / ja ...',
  `subject`         varchar(255) NULL     DEFAULT ''     COMMENT '主题',
  `body`            text         NULL                     COMMENT '正文',
  `variables`       json         NULL                     COMMENT '模板变量列表 JSON',
  `creator`         varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`         varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`         bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_template_type` (`template_type`),
  INDEX `idx_language` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 客服模板';
