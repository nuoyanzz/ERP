-- ============================================================
-- ERP 管理系统 数据库脚本（MySQL 8.0+，InnoDB / utf8mb4）
-- 说明：软删除场景下的唯一约束通过生成列实现
--  （未删除时取业务编码，已删除时取 记录ID，保证编码可复用）
-- 说明：本脚本可重复执行（幂等），索引通过存储过程创建
-- ============================================================

CREATE DATABASE IF NOT EXISTS erp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE erp;

-- ------------------------------------------------------------
-- 索引创建辅助过程（MySQL 8 不支持 CREATE INDEX IF NOT EXISTS）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS erp_add_column;
DROP PROCEDURE IF EXISTS erp_add_index;
DROP PROCEDURE IF EXISTS erp_add_unique_index;

DELIMITER $$
CREATE PROCEDURE erp_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(512))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE erp_add_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns VARCHAR(512))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE erp_add_unique_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns VARCHAR(512))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('CREATE UNIQUE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    email       VARCHAR(100),
    phone       VARCHAR(20),
    dept_id     BIGINT,
    status      SMALLINT     NOT NULL DEFAULT 0,
    remark      VARCHAR(255),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
  `sys_user_username_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `username` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_sys_user_username` (`sys_user_username_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_user', 'idx_sys_user_dept', 'dept_id');
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT PRIMARY KEY,
    role_code   VARCHAR(50)  NOT NULL,
    role_name   VARCHAR(50)  NOT NULL,
    data_scope  SMALLINT     NOT NULL DEFAULT 1,
    status      SMALLINT     NOT NULL DEFAULT 0,
    remark      VARCHAR(255),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
  `sys_role_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `role_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_sys_role_code` (`sys_role_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    menu_name   VARCHAR(50)  NOT NULL,
    menu_type   SMALLINT     NOT NULL DEFAULT 1,
    perms       VARCHAR(100),
    path        VARCHAR(200),
    component   VARCHAR(200),
    icon        VARCHAR(100),
    sort_no     INT          NOT NULL DEFAULT 0,
    visible     SMALLINT     NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_menu', 'idx_sys_menu_parent', 'parent_id');
CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_user_role', 'idx_sys_user_role_user', 'user_id');
CALL erp_add_index('sys_user_role', 'idx_sys_user_role_role', 'role_id');
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_role_menu', 'idx_sys_role_menu_role', 'role_id');
CALL erp_add_index('sys_role_menu', 'idx_sys_role_menu_menu', 'menu_id');
CREATE TABLE IF NOT EXISTS md_material (
    id            BIGINT PRIMARY KEY,
    material_code VARCHAR(50)  NOT NULL,
    material_name VARCHAR(100) NOT NULL,
    spec          VARCHAR(200),
    category_id   BIGINT,
    unit          VARCHAR(20),
    track_batch   SMALLINT     NOT NULL DEFAULT 0,
    track_sn      SMALLINT     NOT NULL DEFAULT 0,
    cost_price    DECIMAL(19,4) NOT NULL DEFAULT 0,
    safe_stock    DECIMAL(19,4) NOT NULL DEFAULT 0,
    status        SMALLINT     NOT NULL DEFAULT 0,
    remark        VARCHAR(255),
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0,
  `md_material_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `material_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_material_code` (`md_material_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('md_material', 'idx_md_material_category', 'category_id');
CREATE TABLE IF NOT EXISTS md_material_category (
    id            BIGINT PRIMARY KEY,
    category_code VARCHAR(50)  NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    parent_id     BIGINT       NOT NULL DEFAULT 0,
    sort_no       INT          NOT NULL DEFAULT 0,
    status        SMALLINT     NOT NULL DEFAULT 0,
    remark        VARCHAR(255),
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0,
  `md_material_category_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `category_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_material_category_code` (`md_material_category_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('md_material_category', 'idx_md_material_category_parent', 'parent_id');
CREATE TABLE IF NOT EXISTS md_customer (
    id            BIGINT PRIMARY KEY,
    customer_code VARCHAR(50)  NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    credit_limit  DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency      VARCHAR(10),
    tax_no        VARCHAR(50),
    contact_name  VARCHAR(50),
    contact_phone VARCHAR(20),
    address       VARCHAR(255),
    sales_dept_id BIGINT,
    status        SMALLINT     NOT NULL DEFAULT 0,
    remark        VARCHAR(255),
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0,
  `md_customer_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `customer_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_customer_code` (`md_customer_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS md_supplier (
    id            BIGINT PRIMARY KEY,
    supplier_code VARCHAR(50)  NOT NULL,
    supplier_name VARCHAR(100) NOT NULL,
    tax_no        VARCHAR(50),
    settle_type   VARCHAR(50),
    level         SMALLINT     NOT NULL DEFAULT 3,
    contact_name  VARCHAR(50),
    contact_phone VARCHAR(20),
    address       VARCHAR(255),
    status        SMALLINT     NOT NULL DEFAULT 0,
    remark        VARCHAR(255),
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0,
  `md_supplier_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `supplier_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_supplier_code` (`md_supplier_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS md_warehouse (
    id              BIGINT PRIMARY KEY,
    warehouse_code  VARCHAR(50)  NOT NULL,
    warehouse_name  VARCHAR(100) NOT NULL,
    wh_type         SMALLINT     NOT NULL DEFAULT 1,
    location        VARCHAR(200),
    status          SMALLINT     NOT NULL DEFAULT 0,
    remark          VARCHAR(255),
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
  `md_warehouse_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `warehouse_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_warehouse_code` (`md_warehouse_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fi_account (
    id              BIGINT PRIMARY KEY,
    subject_code    VARCHAR(20)  NOT NULL,
    subject_name    VARCHAR(100) NOT NULL,
    parent_id       BIGINT       NOT NULL DEFAULT 0,
    category        SMALLINT     NOT NULL DEFAULT 1,
    balance_dir     SMALLINT     NOT NULL DEFAULT 1,
    assist_customer SMALLINT     NOT NULL DEFAULT 0,
    assist_supplier SMALLINT     NOT NULL DEFAULT 0,
    assist_dept     SMALLINT     NOT NULL DEFAULT 0,
    assist_project  SMALLINT     NOT NULL DEFAULT 0,
    currency        VARCHAR(10),
    status          SMALLINT     NOT NULL DEFAULT 0,
    remark          VARCHAR(255),
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
  `fi_account_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `subject_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_fi_account_code` (`fi_account_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_account', 'idx_fi_account_parent', 'parent_id');
CREATE TABLE IF NOT EXISTS fi_period (
    id           BIGINT PRIMARY KEY,
    period_year  INT          NOT NULL,
    period_no    INT          NOT NULL,
    period_month VARCHAR(7)   NOT NULL,
    status       SMALLINT     NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT     NOT NULL DEFAULT 0,
  `fi_period_month_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `period_month` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_fi_period_month` (`fi_period_month_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fi_voucher (
    id               BIGINT PRIMARY KEY,
    voucher_no       VARCHAR(30)   NOT NULL,
    word             VARCHAR(10)   NOT NULL DEFAULT '记',
    voucher_date     DATE          NOT NULL,
    period           VARCHAR(7)    NOT NULL,
    attach_count     INT           NOT NULL DEFAULT 0,
    total_debit      DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_credit     DECIMAL(19,4) NOT NULL DEFAULT 0,
    status           SMALLINT      NOT NULL DEFAULT 0,
    red_flush        SMALLINT      NOT NULL DEFAULT 0,
    source_voucher_id BIGINT,
    summary          VARCHAR(500),
    submit_by        BIGINT,
    submit_time      DATETIME,
    audit_by         BIGINT,
    audit_time       DATETIME,
    post_by          BIGINT,
    post_time        DATETIME,
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by        BIGINT,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    deleted          SMALLINT     NOT NULL DEFAULT 0,
  `fi_voucher_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `voucher_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_fi_voucher_no` (`fi_voucher_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_voucher', 'idx_fi_voucher_period', 'period');
CALL erp_add_index('fi_voucher', 'idx_fi_voucher_date', 'voucher_date');
CALL erp_add_index('fi_voucher', 'idx_fi_voucher_status', 'status');
CREATE TABLE IF NOT EXISTS fi_voucher_entry (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    voucher_id BIGINT        NOT NULL,
    account_id BIGINT        NOT NULL,
    direction  SMALLINT      NOT NULL,
    amount     DECIMAL(19,4) NOT NULL,
    summary    VARCHAR(255),
    assist     VARCHAR(500),
    sort_no    INT           NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_voucher_entry', 'idx_fi_voucher_entry_voucher', 'voucher_id');
CALL erp_add_index('fi_voucher_entry', 'idx_fi_voucher_entry_account', 'account_id');
CREATE TABLE IF NOT EXISTS ic_stock (
    id             BIGINT PRIMARY KEY,
    material_id    BIGINT        NOT NULL,
    warehouse_id   BIGINT        NOT NULL,
    batch_no       VARCHAR(50)   NOT NULL DEFAULT '',
    qty            DECIMAL(19,4) NOT NULL DEFAULT 0,
    locked_qty     DECIMAL(19,4) NOT NULL DEFAULT 0,
    available_qty  DECIMAL(19,4) NOT NULL DEFAULT 0,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT      NOT NULL DEFAULT 0,
  `ic_stock_key_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN CONCAT('|', `material_id`, '|', `warehouse_id`, '|', `batch_no`) ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ic_stock_key` (`ic_stock_key_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ic_transaction (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id  BIGINT        NOT NULL,
    warehouse_id BIGINT        NOT NULL,
    batch_no     VARCHAR(50)   NOT NULL DEFAULT '',
    tran_type    VARCHAR(30)   NOT NULL,
    direction    SMALLINT      NOT NULL DEFAULT 1,
    qty          DECIMAL(19,4) NOT NULL,
    biz_id       BIGINT,
    biz_no       VARCHAR(30),
    biz_date     DATE,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ic_transaction', 'idx_ic_transaction_material', 'material_id');
CALL erp_add_index('ic_transaction', 'idx_ic_transaction_biz', 'tran_type, biz_id');
CALL erp_add_index('ic_transaction', 'idx_ic_transaction_date', 'biz_date');
CREATE TABLE IF NOT EXISTS pa_order (
    id           BIGINT PRIMARY KEY,
    order_no     VARCHAR(30)   NOT NULL,
    supplier_id  BIGINT        NOT NULL,
    dept_id      BIGINT,
    order_date   DATE          NOT NULL,
    delivery_date DATE,
    status       SMALLINT      NOT NULL DEFAULT 0,
    total_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    audit_by     BIGINT,
    audit_time   DATETIME,
    close_reason VARCHAR(255),
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `pa_order_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `order_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pa_order_no` (`pa_order_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pa_order_entry (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    material_id  BIGINT        NOT NULL,
    qty          DECIMAL(19,4) NOT NULL,
    price        DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_rate     DECIMAL(9,4)  NOT NULL DEFAULT 0,
    amount       DECIMAL(19,4) NOT NULL DEFAULT 0,
    received_qty DECIMAL(19,4) NOT NULL DEFAULT 0,
    remark       VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pa_order_entry', 'idx_pa_order_entry_order', 'order_id');
CALL erp_add_index('pa_order_entry', 'idx_pa_order_entry_material', 'material_id');
CREATE TABLE IF NOT EXISTS pa_instock (
    id             BIGINT PRIMARY KEY,
    instock_no     VARCHAR(30)   NOT NULL,
    order_id       BIGINT,
    supplier_id    BIGINT        NOT NULL,
    warehouse_id   BIGINT        NOT NULL,
    instock_date   DATE          NOT NULL,
    status         SMALLINT      NOT NULL DEFAULT 0,
    voucher_status SMALLINT      NOT NULL DEFAULT 0,
    voucher_id     BIGINT,
    total_amount   DECIMAL(19,4) NOT NULL DEFAULT 0,
    audit_by       BIGINT,
    audit_time     DATETIME,
    remark         VARCHAR(255),
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT      NOT NULL DEFAULT 0,
  `pa_instock_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `instock_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pa_instock_no` (`pa_instock_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pa_instock', 'idx_pa_instock_order', 'order_id');
CREATE TABLE IF NOT EXISTS pa_instock_entry (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    instock_id     BIGINT        NOT NULL,
    order_entry_id BIGINT,
    material_id    BIGINT        NOT NULL,
    qty            DECIMAL(19,4) NOT NULL,
    price          DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_rate       DECIMAL(9,4)  NOT NULL DEFAULT 0,
    amount         DECIMAL(19,4) NOT NULL DEFAULT 0,
    returned_qty   DECIMAL(19,4) NOT NULL DEFAULT 0,
    batch_no       VARCHAR(50)   NOT NULL DEFAULT '',
    remark         VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pa_instock_entry', 'idx_pa_instock_entry_instock', 'instock_id');
CALL erp_add_index('pa_instock_entry', 'idx_pa_instock_entry_material', 'material_id');
CREATE TABLE IF NOT EXISTS fi_biz_account_rule (
    id           BIGINT PRIMARY KEY,
    biz_type     VARCHAR(30)  NOT NULL,
    rule_key     VARCHAR(30)  NOT NULL,
    subject_code VARCHAR(20)  NOT NULL,
    remark       VARCHAR(255),
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT     NOT NULL DEFAULT 0,
  `fi_biz_account_rule_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN CONCAT('|', `biz_type`, '|', `rule_key`) ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_fi_biz_account_rule` (`fi_biz_account_rule_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sa_order (
    id           BIGINT PRIMARY KEY,
    order_no     VARCHAR(30)   NOT NULL,
    customer_id  BIGINT        NOT NULL,
    dept_id      BIGINT,
    order_date   DATE          NOT NULL,
    delivery_date DATE,
    status       SMALLINT      NOT NULL DEFAULT 0,
    total_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    audit_by     BIGINT,
    audit_time   DATETIME,
    close_reason VARCHAR(255),
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `sa_order_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `order_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_sa_order_no` (`sa_order_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sa_order_entry (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT        NOT NULL,
    material_id   BIGINT        NOT NULL,
    qty           DECIMAL(19,4) NOT NULL,
    price         DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_rate      DECIMAL(9,4)  NOT NULL DEFAULT 0,
    amount        DECIMAL(19,4) NOT NULL DEFAULT 0,
    delivered_qty DECIMAL(19,4) NOT NULL DEFAULT 0,
    remark        VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sa_order_entry', 'idx_sa_order_entry_order', 'order_id');
CALL erp_add_index('sa_order_entry', 'idx_sa_order_entry_material', 'material_id');
CREATE TABLE IF NOT EXISTS sa_outstock (
    id             BIGINT PRIMARY KEY,
    outstock_no    VARCHAR(30)   NOT NULL,
    order_id       BIGINT,
    customer_id    BIGINT        NOT NULL,
    warehouse_id   BIGINT        NOT NULL,
    outstock_date  DATE          NOT NULL,
    status         SMALLINT      NOT NULL DEFAULT 0,
    voucher_status SMALLINT      NOT NULL DEFAULT 0,
    voucher_id     BIGINT,
    total_cost     DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_amount   DECIMAL(19,4) NOT NULL DEFAULT 0,
    audit_by       BIGINT,
    audit_time     DATETIME,
    remark         VARCHAR(255),
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT      NOT NULL DEFAULT 0,
  `sa_outstock_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `outstock_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_sa_outstock_no` (`sa_outstock_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sa_outstock', 'idx_sa_outstock_order', 'order_id');
CREATE TABLE IF NOT EXISTS sa_outstock_entry (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    outstock_id    BIGINT        NOT NULL,
    order_entry_id BIGINT,
    material_id    BIGINT        NOT NULL,
    qty            DECIMAL(19,4) NOT NULL,
    price          DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_rate       DECIMAL(9,4)  NOT NULL DEFAULT 0,
    amount         DECIMAL(19,4) NOT NULL DEFAULT 0,
    cost_price     DECIMAL(19,4) NOT NULL DEFAULT 0,
    cost_amount    DECIMAL(19,4) NOT NULL DEFAULT 0,
    returned_qty   DECIMAL(19,4) NOT NULL DEFAULT 0,
    batch_no       VARCHAR(50)   NOT NULL DEFAULT '',
    remark         VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sa_outstock_entry', 'idx_sa_outstock_entry_outstock', 'outstock_id');
CALL erp_add_index('sa_outstock_entry', 'idx_sa_outstock_entry_material', 'material_id');
CREATE TABLE IF NOT EXISTS ic_transfer (
    id               BIGINT PRIMARY KEY,
    transfer_no      VARCHAR(30)   NOT NULL,
    from_warehouse_id BIGINT       NOT NULL,
    to_warehouse_id   BIGINT       NOT NULL,
    transfer_date    DATE          NOT NULL,
    status           SMALLINT      NOT NULL DEFAULT 0,
    audit_by         BIGINT,
    audit_time       DATETIME,
    remark           VARCHAR(255),
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by        BIGINT,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    deleted          SMALLINT      NOT NULL DEFAULT 0,
  `ic_transfer_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `transfer_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ic_transfer_no` (`ic_transfer_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ic_transfer_entry (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_id  BIGINT        NOT NULL,
    material_id  BIGINT        NOT NULL,
    batch_no     VARCHAR(50)   NOT NULL DEFAULT '',
    qty          DECIMAL(19,4) NOT NULL,
    remark       VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ic_transfer_entry', 'idx_ic_transfer_entry_transfer', 'transfer_id');
CALL erp_add_index('ic_transfer_entry', 'idx_ic_transfer_entry_material', 'material_id');
CREATE TABLE IF NOT EXISTS ic_check (
    id           BIGINT PRIMARY KEY,
    check_no     VARCHAR(30)   NOT NULL,
    warehouse_id BIGINT        NOT NULL,
    check_date   DATE          NOT NULL,
    status       SMALLINT      NOT NULL DEFAULT 0,
    gain_qty     DECIMAL(19,4) NOT NULL DEFAULT 0,
    loss_qty     DECIMAL(19,4) NOT NULL DEFAULT 0,
    voucher_id   BIGINT,
    audit_by     BIGINT,
    audit_time   DATETIME,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `ic_check_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `check_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ic_check_no` (`ic_check_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ic_check_entry (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_id    BIGINT        NOT NULL,
    material_id BIGINT        NOT NULL,
    batch_no    VARCHAR(50)   NOT NULL DEFAULT '',
    book_qty    DECIMAL(19,4) NOT NULL DEFAULT 0,
    count_qty   DECIMAL(19,4) NOT NULL,
    gain_qty    DECIMAL(19,4) NOT NULL DEFAULT 0,
    loss_qty    DECIMAL(19,4) NOT NULL DEFAULT 0,
    remark      VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ic_check_entry', 'idx_ic_check_entry_check', 'check_id');
CALL erp_add_index('ic_check_entry', 'idx_ic_check_entry_material', 'material_id');
CREATE TABLE IF NOT EXISTS pa_return (
    id                BIGINT PRIMARY KEY,
    return_no         VARCHAR(30)   NOT NULL,
    source_instock_id BIGINT        NOT NULL,
    supplier_id       BIGINT        NOT NULL,
    warehouse_id      BIGINT        NOT NULL,
    return_date       DATE          NOT NULL,
    status            SMALLINT      NOT NULL DEFAULT 0,
    voucher_status    SMALLINT      NOT NULL DEFAULT 0,
    voucher_id        BIGINT,
    total_amount      DECIMAL(19,4) NOT NULL DEFAULT 0,
    audit_by          BIGINT,
    audit_time        DATETIME,
    remark            VARCHAR(255),
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by         BIGINT,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT,
    deleted           SMALLINT      NOT NULL DEFAULT 0,
  `pa_return_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `return_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pa_return_no` (`pa_return_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pa_return', 'idx_pa_return_source', 'source_instock_id');
CREATE TABLE IF NOT EXISTS pa_return_entry (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    return_id       BIGINT        NOT NULL,
    source_entry_id BIGINT        NOT NULL,
    material_id     BIGINT        NOT NULL,
    qty             DECIMAL(19,4) NOT NULL,
    price           DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(9,4)  NOT NULL DEFAULT 0,
    amount          DECIMAL(19,4) NOT NULL DEFAULT 0,
    batch_no        VARCHAR(50)   NOT NULL DEFAULT '',
    remark          VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pa_return_entry', 'idx_pa_return_entry_return', 'return_id');
CALL erp_add_index('pa_return_entry', 'idx_pa_return_entry_source', 'source_entry_id');
CREATE TABLE IF NOT EXISTS sa_return (
    id                BIGINT PRIMARY KEY,
    return_no         VARCHAR(30)   NOT NULL,
    source_outstock_id BIGINT       NOT NULL,
    customer_id       BIGINT        NOT NULL,
    warehouse_id      BIGINT        NOT NULL,
    return_date       DATE          NOT NULL,
    status            SMALLINT      NOT NULL DEFAULT 0,
    voucher_status    SMALLINT      NOT NULL DEFAULT 0,
    voucher_id        BIGINT,
    total_cost        DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_amount      DECIMAL(19,4) NOT NULL DEFAULT 0,
    audit_by          BIGINT,
    audit_time        DATETIME,
    remark            VARCHAR(255),
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by         BIGINT,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT,
    deleted           SMALLINT      NOT NULL DEFAULT 0,
  `sa_return_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `return_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_sa_return_no` (`sa_return_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sa_return', 'idx_sa_return_source', 'source_outstock_id');
CREATE TABLE IF NOT EXISTS sa_return_entry (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    return_id       BIGINT        NOT NULL,
    source_entry_id BIGINT        NOT NULL,
    material_id     BIGINT        NOT NULL,
    qty             DECIMAL(19,4) NOT NULL,
    price           DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(9,4)  NOT NULL DEFAULT 0,
    amount          DECIMAL(19,4) NOT NULL DEFAULT 0,
    cost_price      DECIMAL(19,4) NOT NULL DEFAULT 0,
    cost_amount     DECIMAL(19,4) NOT NULL DEFAULT 0,
    batch_no        VARCHAR(50)   NOT NULL DEFAULT '',
    remark          VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sa_return_entry', 'idx_sa_return_entry_return', 'return_id');
CALL erp_add_index('sa_return_entry', 'idx_sa_return_entry_source', 'source_entry_id');
CREATE TABLE IF NOT EXISTS wf_definition (
    id             BIGINT PRIMARY KEY,
    definition_key VARCHAR(50)  NOT NULL,
    definition_name VARCHAR(100) NOT NULL,
    biz_type       VARCHAR(30),
    description    VARCHAR(255),
    status         SMALLINT     NOT NULL DEFAULT 0,
    version        INT          NOT NULL DEFAULT 1,
    remark         VARCHAR(255),
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT     NOT NULL DEFAULT 0,
  `wf_definition_key_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `definition_key` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_wf_definition_key` (`wf_definition_key_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS wf_node (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id  BIGINT       NOT NULL,
    node_key       VARCHAR(50)  NOT NULL,
    node_name      VARCHAR(100) NOT NULL,
    node_type      SMALLINT     NOT NULL DEFAULT 2,
    sort_no        INT          NOT NULL DEFAULT 0,
    approver_type  SMALLINT     NOT NULL DEFAULT 1,
    approver_value VARCHAR(200),
    multi_approve  SMALLINT     NOT NULL DEFAULT 1,
    remark         VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('wf_node', 'idx_wf_node_definition', 'definition_id');
CREATE TABLE IF NOT EXISTS wf_instance (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id  BIGINT       NOT NULL,
    biz_type       VARCHAR(30),
    biz_id         BIGINT,
    title          VARCHAR(200) NOT NULL,
    starter_id     BIGINT       NOT NULL,
    current_node_id BIGINT,
    status         SMALLINT     NOT NULL DEFAULT 0,
    finish_time    DATETIME,
    remark         VARCHAR(255),
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT     NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('wf_instance', 'idx_wf_instance_biz', 'biz_type, biz_id');
CALL erp_add_index('wf_instance', 'idx_wf_instance_starter', 'starter_id');
CREATE TABLE IF NOT EXISTS wf_task (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id    BIGINT       NOT NULL,
    node_id        BIGINT       NOT NULL,
    assignee_id    BIGINT       NOT NULL,
    status         SMALLINT     NOT NULL DEFAULT 0,
    comment        VARCHAR(500),
    operate_by     BIGINT,
    operate_time   DATETIME,
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT     NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('wf_task', 'idx_wf_task_instance', 'instance_id');
CALL erp_add_index('wf_task', 'idx_wf_task_assignee', 'assignee_id, status');
CREATE TABLE IF NOT EXISTS ar_receivable (
    id              BIGINT PRIMARY KEY,
    receivable_no   VARCHAR(30)   NOT NULL,
    customer_id     BIGINT        NOT NULL,
    source_type     VARCHAR(30),
    source_id       BIGINT,
    source_no       VARCHAR(30),
    amount          DECIMAL(19,4) NOT NULL DEFAULT 0,
    received_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    status          SMALLINT      NOT NULL DEFAULT 0,
    due_date        DATE,
    remark          VARCHAR(255),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    deleted         SMALLINT      NOT NULL DEFAULT 0,
  `ar_receivable_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `receivable_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ar_receivable_no` (`ar_receivable_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ar_receivable', 'idx_ar_receivable_customer', 'customer_id');
CALL erp_add_index('ar_receivable', 'idx_ar_receivable_source', 'source_type, source_id');
CREATE TABLE IF NOT EXISTS ar_receipt (
    id           BIGINT PRIMARY KEY,
    receipt_no   VARCHAR(30)   NOT NULL,
    customer_id  BIGINT        NOT NULL,
    receipt_date DATE          NOT NULL,
    amount       DECIMAL(19,4) NOT NULL,
    status       SMALLINT      NOT NULL DEFAULT 0,
    voucher_id   BIGINT,
    audit_by     BIGINT,
    audit_time   DATETIME,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `ar_receipt_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `receipt_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ar_receipt_no` (`ar_receipt_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ar_receipt', 'idx_ar_receipt_customer', 'customer_id');
CREATE TABLE IF NOT EXISTS ar_verify (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_id    BIGINT        NOT NULL,
    receivable_id BIGINT        NOT NULL,
    amount        DECIMAL(19,4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ar_verify', 'idx_ar_verify_receipt', 'receipt_id');
CALL erp_add_index('ar_verify', 'idx_ar_verify_receivable', 'receivable_id');
CREATE TABLE IF NOT EXISTS ap_payable (
    id           BIGINT PRIMARY KEY,
    payable_no   VARCHAR(30)   NOT NULL,
    supplier_id  BIGINT        NOT NULL,
    source_type  VARCHAR(30),
    source_id    BIGINT,
    source_no    VARCHAR(30),
    amount       DECIMAL(19,4) NOT NULL DEFAULT 0,
    paid_amount  DECIMAL(19,4) NOT NULL DEFAULT 0,
    status       SMALLINT      NOT NULL DEFAULT 0,
    due_date     DATE,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `ap_payable_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `payable_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ap_payable_no` (`ap_payable_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ap_payable', 'idx_ap_payable_supplier', 'supplier_id');
CALL erp_add_index('ap_payable', 'idx_ap_payable_source', 'source_type, source_id');
CREATE TABLE IF NOT EXISTS ap_payment (
    id           BIGINT PRIMARY KEY,
    payment_no   VARCHAR(30)   NOT NULL,
    supplier_id  BIGINT        NOT NULL,
    payment_date DATE          NOT NULL,
    amount       DECIMAL(19,4) NOT NULL,
    status       SMALLINT      NOT NULL DEFAULT 0,
    voucher_id   BIGINT,
    audit_by     BIGINT,
    audit_time   DATETIME,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `ap_payment_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `payment_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ap_payment_no` (`ap_payment_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ap_payment', 'idx_ap_payment_supplier', 'supplier_id');
CREATE TABLE IF NOT EXISTS ap_verify (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id  BIGINT        NOT NULL,
    payable_id  BIGINT        NOT NULL,
    amount      DECIMAL(19,4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('ap_verify', 'idx_ap_verify_payment', 'payment_id');
CALL erp_add_index('ap_verify', 'idx_ap_verify_payable', 'payable_id');
CREATE TABLE IF NOT EXISTS pm_bom (
    id           BIGINT PRIMARY KEY,
    bom_no       VARCHAR(30)   NOT NULL,
    material_id  BIGINT        NOT NULL,
    version      VARCHAR(20)   NOT NULL DEFAULT 'V1',
    status       SMALLINT      NOT NULL DEFAULT 1,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `pm_bom_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `bom_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pm_bom_no` (`pm_bom_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_bom', 'idx_pm_bom_material', 'material_id');
CREATE TABLE IF NOT EXISTS pm_bom_entry (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    bom_id      BIGINT        NOT NULL,
    material_id BIGINT        NOT NULL,
    qty         DECIMAL(19,4) NOT NULL,
    loss_rate   DECIMAL(9,4)  NOT NULL DEFAULT 0,
    remark      VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_bom_entry', 'idx_pm_bom_entry_bom', 'bom_id');
CREATE TABLE IF NOT EXISTS pm_mo (
    id             BIGINT PRIMARY KEY,
    mo_no          VARCHAR(30)   NOT NULL,
    material_id    BIGINT        NOT NULL,
    bom_id         BIGINT        NOT NULL,
    qty            DECIMAL(19,4) NOT NULL,
    produced_qty   DECIMAL(19,4) NOT NULL DEFAULT 0,
    warehouse_id   BIGINT        NOT NULL,
    plan_start_date DATE         NOT NULL,
    plan_end_date   DATE,
    status         SMALLINT      NOT NULL DEFAULT 0,
    audit_by       BIGINT,
    audit_time     DATETIME,
    remark         VARCHAR(255),
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT      NOT NULL DEFAULT 0,
  `pm_mo_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `mo_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pm_mo_no` (`pm_mo_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_mo', 'idx_pm_mo_material', 'material_id');
CREATE TABLE IF NOT EXISTS pm_mo_entry (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    mo_id        BIGINT        NOT NULL,
    bom_entry_id BIGINT,
    material_id  BIGINT        NOT NULL,
    require_qty  DECIMAL(19,4) NOT NULL DEFAULT 0,
    picked_qty   DECIMAL(19,4) NOT NULL DEFAULT 0,
    remark       VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_mo_entry', 'idx_pm_mo_entry_mo', 'mo_id');
CREATE TABLE IF NOT EXISTS pm_pick (
    id           BIGINT PRIMARY KEY,
    pick_no      VARCHAR(30)   NOT NULL,
    mo_id        BIGINT        NOT NULL,
    warehouse_id BIGINT        NOT NULL,
    pick_date    DATE          NOT NULL,
    status       SMALLINT      NOT NULL DEFAULT 0,
    audit_by     BIGINT,
    audit_time   DATETIME,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `pm_pick_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `pick_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pm_pick_no` (`pm_pick_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_pick', 'idx_pm_pick_mo', 'mo_id');
CREATE TABLE IF NOT EXISTS pm_pick_entry (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    pick_id      BIGINT        NOT NULL,
    mo_entry_id  BIGINT        NOT NULL,
    material_id  BIGINT        NOT NULL,
    qty          DECIMAL(19,4) NOT NULL,
    cost_price   DECIMAL(19,4) NOT NULL DEFAULT 0,
    cost_amount  DECIMAL(19,4) NOT NULL DEFAULT 0,
    batch_no     VARCHAR(50)   NOT NULL DEFAULT '',
    remark       VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_pick_entry', 'idx_pm_pick_entry_pick', 'pick_id');
CALL erp_add_index('pm_pick_entry', 'idx_pm_pick_entry_mo_entry', 'mo_entry_id');
CREATE TABLE IF NOT EXISTS pm_finish (
    id           BIGINT PRIMARY KEY,
    finish_no    VARCHAR(30)   NOT NULL,
    mo_id        BIGINT        NOT NULL,
    warehouse_id BIGINT        NOT NULL,
    finish_date  DATE          NOT NULL,
    qty          DECIMAL(19,4) NOT NULL,
    cost_amount  DECIMAL(19,4) NOT NULL DEFAULT 0,
    status       SMALLINT      NOT NULL DEFAULT 0,
    voucher_id   BIGINT,
    audit_by     BIGINT,
    audit_time   DATETIME,
    remark       VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT      NOT NULL DEFAULT 0,
  `pm_finish_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `finish_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pm_finish_no` (`pm_finish_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_finish', 'idx_pm_finish_mo', 'mo_id');
CREATE TABLE IF NOT EXISTS pm_mrp_result (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_no         VARCHAR(30)   NOT NULL,
    material_id    BIGINT        NOT NULL,
    suggest_type   SMALLINT      NOT NULL,
    gross_qty      DECIMAL(19,4) NOT NULL DEFAULT 0,
    available_qty  DECIMAL(19,4) NOT NULL DEFAULT 0,
    on_order_qty   DECIMAL(19,4) NOT NULL DEFAULT 0,
    safe_stock     DECIMAL(19,4) NOT NULL DEFAULT 0,
    net_qty        DECIMAL(19,4) NOT NULL DEFAULT 0,
    bom_id         BIGINT,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      BIGINT,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    deleted        SMALLINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_mrp_result', 'idx_pm_mrp_result_run', 'run_no');
CREATE TABLE IF NOT EXISTS hr_org (
    id          BIGINT PRIMARY KEY,
    org_code    VARCHAR(30)  NOT NULL,
    org_name    VARCHAR(100) NOT NULL,
    org_type    SMALLINT     NOT NULL DEFAULT 1,
    parent_id   BIGINT,
    remark      VARCHAR(255),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
  `hr_org_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `org_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_hr_org_code` (`hr_org_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('hr_org', 'idx_hr_org_parent', 'parent_id');
CREATE TABLE IF NOT EXISTS hr_dept (
    id          BIGINT PRIMARY KEY,
    dept_code   VARCHAR(30)  NOT NULL,
    dept_name   VARCHAR(100) NOT NULL,
    org_id      BIGINT,
    parent_id   BIGINT,
    remark      VARCHAR(255),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
  `hr_dept_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `dept_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_hr_dept_code` (`hr_dept_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('hr_dept', 'idx_hr_dept_parent', 'parent_id');
CALL erp_add_index('hr_dept', 'idx_hr_dept_org', 'org_id');
CREATE TABLE IF NOT EXISTS hr_employee (
    id            BIGINT PRIMARY KEY,
    employee_no   VARCHAR(30)   NOT NULL,
    employee_name VARCHAR(50)   NOT NULL,
    gender        SMALLINT     NOT NULL DEFAULT 1,
    dept_id       BIGINT,
    position      VARCHAR(50),
    entry_date    DATE,
    leave_date    DATE,
    phone         VARCHAR(20),
    email         VARCHAR(100),
    status        SMALLINT     NOT NULL DEFAULT 0,
    remark        VARCHAR(255),
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT      NOT NULL DEFAULT 0,
  `hr_employee_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `employee_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_hr_employee_no` (`hr_employee_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('hr_employee', 'idx_hr_employee_dept', 'dept_id');
CREATE TABLE IF NOT EXISTS hr_salary (
    id               BIGINT PRIMARY KEY,
    salary_no        VARCHAR(30)   NOT NULL,
    period           VARCHAR(7)    NOT NULL,
    employee_id      BIGINT        NOT NULL,
    dept_id          BIGINT,
    base_amount      DECIMAL(19,4) NOT NULL DEFAULT 0,
    merit_amount     DECIMAL(19,4) NOT NULL DEFAULT 0,
    allowance_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    deduct_amount    DECIMAL(19,4) NOT NULL DEFAULT 0,
    social_amount    DECIMAL(19,4) NOT NULL DEFAULT 0,
    net_amount       DECIMAL(19,4) NOT NULL DEFAULT 0,
    status           SMALLINT      NOT NULL DEFAULT 0,
    voucher_id       BIGINT,
    audit_by         BIGINT,
    audit_time       DATETIME,
    remark           VARCHAR(255),
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by        BIGINT,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    deleted          SMALLINT      NOT NULL DEFAULT 0,
  `hr_salary_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `salary_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_hr_salary_no` (`hr_salary_no_uniq`),
  `hr_salary_period_employee_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN CONCAT('|', `period`, '|', `employee_id`) ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_hr_salary_period_employee` (`hr_salary_period_employee_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('hr_salary', 'idx_hr_salary_employee', 'employee_id');
CREATE TABLE IF NOT EXISTS hr_salary_entry (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_id  BIGINT        NOT NULL,
    item_type  SMALLINT      NOT NULL,
    item_name  VARCHAR(50)   NOT NULL,
    amount     DECIMAL(19,4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('hr_salary_entry', 'idx_hr_salary_entry_salary', 'salary_id');








