-- ----------------------------
-- Amazon Research 模块 DDL
-- ----------------------------

-- 品类细分表 (Niche)
CREATE TABLE IF NOT EXISTS `amazon_niche` (
  `id`                      bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`               bigint       NOT NULL DEFAULT 0     COMMENT 'Tenant ID/租户编号',
  `name`                    varchar(255) NOT NULL DEFAULT ''     COMMENT '品类名称',
  `marketplace_id`          varchar(32)  NOT NULL DEFAULT ''     COMMENT 'Amazon Marketplace ID',
  `category`                varchar(255) NULL     DEFAULT ''     COMMENT '所属类目',
  `status`                  tinyint      NOT NULL DEFAULT 0      COMMENT '状态: 0=草稿, 1=分析中, 2=已完成, 3=已归档',
  `omniscient_score`        decimal(6,2) NULL     DEFAULT NULL   COMMENT '全知评分 0-100',
  `demand_score`            decimal(6,2) NULL     DEFAULT NULL   COMMENT '需求维度评分',
  `competition_score`       decimal(6,2) NULL     DEFAULT NULL   COMMENT '竞争维度评分',
  `profitability_score`     decimal(6,2) NULL     DEFAULT NULL   COMMENT '盈利维度评分',
  `review_moat_score`       decimal(6,2) NULL     DEFAULT NULL   COMMENT '评论护城河评分',
  `price_stability_score`   decimal(6,2) NULL     DEFAULT NULL   COMMENT '价格稳定性评分',
  `seasonality_score`       decimal(6,2) NULL     DEFAULT NULL   COMMENT '季节性评分',
  `organic_rank_score`      decimal(6,2) NULL     DEFAULT NULL   COMMENT '自然排名评分',
  `ad_dependency_score`     decimal(6,2) NULL     DEFAULT NULL   COMMENT '广告依赖度评分',
  `supplier_score`          decimal(6,2) NULL     DEFAULT NULL   COMMENT '供应商评分',
  `score_weights`           json         NULL                     COMMENT '评分权重 JSON {dimension: weight}',
  `creator`                 varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`                 varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`                 bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_marketplace_id` (`marketplace_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 品类细分(Niche)';

-- BSR-销量回归模型参数
CREATE TABLE IF NOT EXISTS `amazon_bsr_regression` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `category_id`       varchar(128) NOT NULL DEFAULT ''     COMMENT '类目 ID',
  `marketplace_id`    varchar(32)  NOT NULL DEFAULT ''     COMMENT 'Amazon Marketplace ID',
  `coefficient_a`     decimal(16,6) NULL    DEFAULT NULL   COMMENT '回归系数 a',
  `coefficient_alpha` decimal(16,6) NULL    DEFAULT NULL   COMMENT '回归指数 alpha',
  `r_squared`         decimal(8,6)  NULL    DEFAULT NULL   COMMENT 'R-squared 拟合优度',
  `sample_size`       int          NULL     DEFAULT NULL   COMMENT '样本量',
  `last_fit_date`     datetime     NULL     DEFAULT NULL   COMMENT '最后拟合日期',
  `creator`           varchar(64)  NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`           varchar(64)  NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_category_marketplace` (`category_id`, `marketplace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon BSR-销量回归模型';

-- 产品机会表
CREATE TABLE IF NOT EXISTS `amazon_product_opportunity` (
  `id`                      bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `niche_id`                bigint        NOT NULL               COMMENT '关联品类 ID',
  `asin`                    varchar(16)   NOT NULL DEFAULT ''     COMMENT 'ASIN',
  `title`                   varchar(512)  NULL     DEFAULT ''     COMMENT '商品标题',
  `price`                   decimal(10,2) NULL     DEFAULT NULL   COMMENT '售价',
  `currency`                varchar(8)    NOT NULL DEFAULT 'USD'  COMMENT '币种',
  `rating`                  decimal(3,2)  NULL     DEFAULT NULL   COMMENT '星级评分',
  `review_count`            int           NULL     DEFAULT NULL   COMMENT '评论数',
  `bsr`                     int           NULL     DEFAULT NULL   COMMENT 'BSR 排名',
  `monthly_search_volume`   bigint        NULL     DEFAULT NULL   COMMENT '月搜索量',
  `estimated_monthly_sales` int           NULL     DEFAULT NULL   COMMENT '预估月销量',
  `profit_margin`           decimal(6,4)  NULL     DEFAULT NULL   COMMENT '利润率',
  `creator`                 varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`                 varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`                 bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_niche_id` (`niche_id`),
  INDEX `idx_asin` (`asin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 产品机会';

-- 财务预测表
CREATE TABLE IF NOT EXISTS `amazon_financial_projection` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `opportunity_id`      bigint        NOT NULL               COMMENT '关联产品机会 ID',
  `startup_cost`        decimal(12,2) NULL     DEFAULT NULL   COMMENT '启动成本',
  `break_even_week`     int           NULL     DEFAULT NULL   COMMENT '回本周数',
  `twelve_month_profit` decimal(12,2) NULL     DEFAULT NULL   COMMENT '12个月累计利润',
  `brand_valuation`     decimal(12,2) NULL     DEFAULT NULL   COMMENT '品牌估值',
  `projection_data`     json          NULL                     COMMENT '52周预测数据 JSON',
  `creator`             varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`             varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`             bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_opportunity_id` (`opportunity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 财务预测';

-- 供应商匹配表
CREATE TABLE IF NOT EXISTS `amazon_supplier_match` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `opportunity_id`  bigint        NOT NULL               COMMENT '关联产品机会 ID',
  `supplier_name`   varchar(255)  NOT NULL DEFAULT ''     COMMENT '供应商名称',
  `supplier_url`    varchar(512)  NULL     DEFAULT ''     COMMENT '供应商链接',
  `min_order_qty`   int           NULL     DEFAULT NULL   COMMENT '最小起订量',
  `unit_price`      decimal(10,2) NULL     DEFAULT NULL   COMMENT '单价',
  `rating`          decimal(3,2)  NULL     DEFAULT NULL   COMMENT '供应商评分',
  `location`        varchar(128)  NULL     DEFAULT ''     COMMENT '所在地',
  `factory_type`    varchar(32)   NULL     DEFAULT ''     COMMENT '工厂类型: FACTORY / TRADING_COMPANY',
  `creator`         varchar(64)   NULL     DEFAULT ''     COMMENT 'Creator/创建者',
  `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time/创建时间',
  `updater`         varchar(64)   NULL     DEFAULT ''     COMMENT 'Updater/更新者',
  `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update/更新时间',
  `deleted`         bit(1)        NOT NULL DEFAULT b'0'   COMMENT 'Soft delete/是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_opportunity_id` (`opportunity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amazon 供应商匹配';
