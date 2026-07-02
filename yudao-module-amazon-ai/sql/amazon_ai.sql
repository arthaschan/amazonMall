-- ----------------------------
-- Amazon AI 模块 DDL
-- ----------------------------

-- AI Token 用量记录表
CREATE TABLE IF NOT EXISTS `amazon_ai_token_usage` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`           bigint        NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `task_type`           varchar(64)   NOT NULL DEFAULT ''     COMMENT 'AI 任务类型 (e.g. listing_generation)',
  `model`               varchar(32)   NOT NULL DEFAULT ''     COMMENT '使用的模型 (e.g. gpt-4o, gpt-4o-mini)',
  `input_tokens`        int           NOT NULL DEFAULT 0      COMMENT '输入(prompt) token 数',
  `output_tokens`       int           NOT NULL DEFAULT 0      COMMENT '输出(completion) token 数',
  `total_tokens`        int           NOT NULL DEFAULT 0      COMMENT '总 token 数 (input + output)',
  `estimated_cost_usd`  double        NULL     DEFAULT NULL   COMMENT '预估费用 (USD)',
  `business_id`         varchar(128)  NULL     DEFAULT ''     COMMENT '业务实体 ID (e.g. listing ID, ASIN)',
  `user_id`             bigint        NULL     DEFAULT NULL   COMMENT '发起人 ID',
  `year_month`          varchar(8)    NULL     DEFAULT ''     COMMENT '年月 (e.g. 2025-06, 用于聚合查询)',
  `create_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_task_type` (`task_type`),
  INDEX `idx_model` (`model`),
  INDEX `idx_year_month` (`year_month`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_business_id` (`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon AI Token 用量记录';
