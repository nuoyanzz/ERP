-- ============================================================
-- ERP 管理系统 数据库增量迁移脚本 V1.1.0
-- 目标：把系统从「演示可用」提升到「企业可用」
-- 说明：全部使用 ALTER TABLE ... ADD COLUMN IF NOT EXISTS / CREATE TABLE IF NOT EXISTS
--       可重复执行，兼容 MySQL 8.0
-- ============================================================
USE erp;

-- ------------------------------------------------------------
-- 1. 系统管理：操作日志、登录失败锁定、密码策略、角色自定义数据范围
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id            BIGINT PRIMARY KEY,
    module        VARCHAR(30),
    oper_type     VARCHAR(30),
    oper_desc     VARCHAR(255),
    method        VARCHAR(100),
    request_url   VARCHAR(255),
    params        TEXT,
    result        TEXT,
    oper_by       BIGINT,
    oper_username VARCHAR(50),
    oper_ip       VARCHAR(50),
    cost_time     BIGINT,
    success       SMALLINT NOT NULL DEFAULT 1,
    error_msg     VARCHAR(1000),
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_oper_log', 'idx_sys_oper_log_time', 'create_time');
CALL erp_add_index('sys_oper_log', 'idx_sys_oper_log_oper', 'oper_by');
CALL erp_add_index('sys_oper_log', 'idx_sys_oper_log_module', 'module, oper_type');
CREATE TABLE IF NOT EXISTS sys_role_dept (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_role_dept', 'idx_sys_role_dept_role', 'role_id');
CALL erp_add_unique_index('sys_role_dept', 'uk_sys_role_dept', 'role_id, dept_id');
-- 登录失败锁定
CREATE TABLE IF NOT EXISTS sys_login_log (
    id            BIGINT PRIMARY KEY,
    username      VARCHAR(50) NOT NULL,
    login_ip      VARCHAR(50),
    status        SMALLINT NOT NULL DEFAULT 0,
    error_msg     VARCHAR(255),
    login_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_login_log', 'idx_sys_login_log_username', 'username, login_time');
-- 补齐 BaseEntity 的审计字段，避免插入时列缺失
CALL erp_add_column('sys_login_log', 'create_by', 'BIGINT');
CALL erp_add_column('sys_login_log', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('sys_login_log', 'update_by', 'BIGINT');
CALL erp_add_column('sys_user', 'fail_count', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('sys_user', 'lock_until', 'DATETIME');
CALL erp_add_column('sys_user', 'pwd_update_time', 'DATETIME');
CALL erp_add_column('sys_user', 'last_login_time', 'DATETIME');
CALL erp_add_column('sys_user', 'last_login_ip', 'VARCHAR(50)');
-- ------------------------------------------------------------
-- 2. 财务域：期初余额、汇率、试算平衡、结转、辅助核算明细
-- ------------------------------------------------------------

-- 科目期初余额（承接存量账套的关键）
CREATE TABLE IF NOT EXISTS fi_opening_balance (
    id           BIGINT PRIMARY KEY,
    sob_id       BIGINT NOT NULL DEFAULT 1,
    account_id   BIGINT NOT NULL,
    period       VARCHAR(7) NOT NULL,
    currency     VARCHAR(10),
    debit_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    credit_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    aux_type     VARCHAR(20),
    aux_id       BIGINT,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_opening_balance', 'idx_fi_opening_account', 'account_id, period');
CALL erp_add_index('fi_opening_balance', 'idx_fi_opening_aux', 'aux_type, aux_id');
-- 汇率表
CREATE TABLE IF NOT EXISTS fi_exchange_rate (
    id           BIGINT PRIMARY KEY,
    currency     VARCHAR(10) NOT NULL,
    period       VARCHAR(7) NOT NULL,
    rate         DECIMAL(19,6) NOT NULL DEFAULT 1,
    rate_type    SMALLINT NOT NULL DEFAULT 1,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_exchange_rate', 'idx_fi_rate_currency', 'currency, period');
-- 凭证分录补外币三列（原币金额 / 汇率 / 币别）
CALL erp_add_column('fi_voucher_entry', 'currency', 'VARCHAR(10)');
CALL erp_add_column('fi_voucher_entry', 'original_amount', 'DECIMAL(19,4)');
CALL erp_add_column('fi_voucher_entry', 'exchange_rate', 'DECIMAL(19,6)');
-- 辅助核算改为结构化三列，assist 保留兼容
CALL erp_add_column('fi_voucher_entry', 'aux_type', 'VARCHAR(20)');
CALL erp_add_column('fi_voucher_entry', 'aux_id', 'BIGINT');
CALL erp_add_index('fi_voucher_entry', 'idx_fi_entry_aux', 'aux_type, aux_id');
-- 凭证：作废 / 反记账留痕 / 现金流量分类
CALL erp_add_column('fi_voucher', 'void_reason', 'VARCHAR(255)');
CALL erp_add_column('fi_voucher', 'void_by', 'BIGINT');
CALL erp_add_column('fi_voucher', 'void_time', 'DATETIME');
CALL erp_add_column('fi_voucher', 'unpost_by', 'BIGINT');
CALL erp_add_column('fi_voucher', 'unpost_time', 'DATETIME');
CALL erp_add_column('fi_voucher', 'source_type', 'VARCHAR(30)');
CALL erp_add_column('fi_voucher', 'source_id', 'BIGINT');
CALL erp_add_index('fi_voucher', 'idx_fi_voucher_source', 'source_type, source_id');
-- 会计科目：明细标记、现金流量分类、是否损益结转、数量核算
CALL erp_add_column('fi_account', 'is_leaf', 'TINYINT NOT NULL DEFAULT 1');
CALL erp_add_column('fi_account', 'cash_flow_type', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('fi_account', 'carry_forward', 'TINYINT NOT NULL DEFAULT 1');
CALL erp_add_column('fi_account', 'quantity_account', 'TINYINT NOT NULL DEFAULT 0');
CALL erp_add_column('fi_account', 'unit', 'VARCHAR(20)');
CALL erp_add_column('fi_account', 'foreign_currency', 'TINYINT NOT NULL DEFAULT 0');
-- 会计期间：结账留痕
CALL erp_add_column('fi_period', 'close_by', 'BIGINT');
CALL erp_add_column('fi_period', 'close_time', 'DATETIME');
CALL erp_add_column('fi_period', 'open_by', 'BIGINT');
CALL erp_add_column('fi_period', 'open_time', 'DATETIME');
CALL erp_add_column('fi_period', 'carried_forward', 'TINYINT NOT NULL DEFAULT 0');
CALL erp_add_column('fi_period', 'is_year_end', 'TINYINT NOT NULL DEFAULT 0');
-- 科目映射规则：改为借贷双科目 + 金额来源
CALL erp_add_column('fi_biz_account_rule', 'debit_subject_code', 'VARCHAR(20)');
CALL erp_add_column('fi_biz_account_rule', 'credit_subject_code', 'VARCHAR(20)');
CALL erp_add_column('fi_biz_account_rule', 'amount_source', 'VARCHAR(50)');
CALL erp_add_column('fi_biz_account_rule', 'cash_flow_type', 'SMALLINT NOT NULL DEFAULT 0');
-- 期末结转记录（支持反结转与重做）
CREATE TABLE IF NOT EXISTS fi_carry_forward (
    id            BIGINT PRIMARY KEY,
    period        VARCHAR(7) NOT NULL,
    carry_type    SMALLINT NOT NULL DEFAULT 1,
    voucher_id    BIGINT,
    total_amount  DECIMAL(19,4) NOT NULL DEFAULT 0,
    status        SMALLINT NOT NULL DEFAULT 0,
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_carry_forward', 'idx_fi_carry_period', 'period');
-- 试算平衡快照
CREATE TABLE IF NOT EXISTS fi_trial_balance (
    id           BIGINT PRIMARY KEY,
    period       VARCHAR(7) NOT NULL,
    account_id   BIGINT NOT NULL,
    debit_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    credit_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_trial_balance', 'idx_fi_trial_period', 'period');
-- 补齐 BaseEntity 审计字段
CALL erp_add_column('fi_trial_balance', 'create_by', 'BIGINT');
CALL erp_add_column('fi_trial_balance', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('fi_trial_balance', 'update_by', 'BIGINT');
-- 会计科目余额快照（用于期初与报表重述）
CREATE TABLE IF NOT EXISTS fi_account_balance (
    id             BIGINT PRIMARY KEY,
    account_id     BIGINT NOT NULL,
    period         VARCHAR(7) NOT NULL,
    currency       VARCHAR(10),
    debit_amount   DECIMAL(19,4) NOT NULL DEFAULT 0,
    credit_amount  DECIMAL(19,4) NOT NULL DEFAULT 0,
    debit_qty      DECIMAL(19,4) NOT NULL DEFAULT 0,
    credit_qty     DECIMAL(19,4) NOT NULL DEFAULT 0,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('fi_account_balance', 'idx_fi_balance_account_period', 'account_id, period');
-- ------------------------------------------------------------
-- 3. 主数据：单位换算、价格表、批次主数据、序列号、客户银行信息
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS md_unit (
    id          BIGINT PRIMARY KEY,
    unit_code   VARCHAR(30) NOT NULL,
    unit_name   VARCHAR(50) NOT NULL,
    status      SMALLINT NOT NULL DEFAULT 0,
    remark      VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT NOT NULL DEFAULT 0,
  `md_unit_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `unit_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_unit_code` (`md_unit_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS md_unit_convert (
    id           BIGINT PRIMARY KEY,
    material_id  BIGINT NOT NULL,
    from_unit    VARCHAR(20) NOT NULL,
    to_unit      VARCHAR(20) NOT NULL,
    convert_rate DECIMAL(19,6) NOT NULL DEFAULT 1,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('md_unit_convert', 'idx_md_unit_convert_material', 'material_id');
CREATE TABLE IF NOT EXISTS md_price_list (
    id            BIGINT PRIMARY KEY,
    price_no      VARCHAR(30) NOT NULL,
    price_name    VARCHAR(100) NOT NULL,
    customer_id   BIGINT,
    supplier_id   BIGINT,
    material_id   BIGINT NOT NULL,
    price         DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_rate      DECIMAL(9,4) NOT NULL DEFAULT 0,
    currency      VARCHAR(10),
    effective_date DATE,
    expiry_date   DATE,
    status        SMALLINT NOT NULL DEFAULT 0,
    remark        VARCHAR(255),
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT NOT NULL DEFAULT 0,
  `md_price_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `price_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_price_no` (`md_price_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('md_price_list', 'idx_md_price_material', 'material_id, effective_date');
CREATE TABLE IF NOT EXISTS md_batch (
    id            BIGINT PRIMARY KEY,
    batch_no      VARCHAR(50) NOT NULL,
    material_id   BIGINT NOT NULL,
    produce_date  DATE,
    expiry_date   DATE,
    supplier_id   BIGINT,
    quality_status SMALLINT NOT NULL DEFAULT 1,
    status        SMALLINT NOT NULL DEFAULT 0,
    remark        VARCHAR(255),
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted       SMALLINT NOT NULL DEFAULT 0,
  `md_batch_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN CONCAT('|', `material_id`, '|', `batch_no`) ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_batch` (`md_batch_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS md_serial_number (
    id           BIGINT PRIMARY KEY,
    sn_no        VARCHAR(50) NOT NULL,
    material_id  BIGINT NOT NULL,
    batch_no     VARCHAR(50),
    warehouse_id BIGINT,
    customer_id  BIGINT,
    supplier_id  BIGINT,
    status       SMALLINT NOT NULL DEFAULT 1,
    in_time      DATETIME,
    out_time     DATETIME,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `md_sn_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `sn_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_md_serial_number` (`md_sn_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('md_serial_number', 'idx_md_sn_material', 'material_id');
-- 物料：采购/自制属性、提前期、批量、计价方法
CALL erp_add_column('md_material', 'procure_type', 'SMALLINT NOT NULL DEFAULT 1');
CALL erp_add_column('md_material', 'lead_time_days', 'INT NOT NULL DEFAULT 0');
CALL erp_add_column('md_material', 'purchase_lead_days', 'INT NOT NULL DEFAULT 0');
CALL erp_add_column('md_material', 'production_lead_days', 'INT NOT NULL DEFAULT 0');
CALL erp_add_column('md_material', 'min_order_qty', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('md_material', 'lot_size', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('md_material', 'lot_period_days', 'INT NOT NULL DEFAULT 0');
CALL erp_add_column('md_material', 'cost_method', 'SMALLINT NOT NULL DEFAULT 1');
CALL erp_add_column('md_material', 'default_supplier_id', 'BIGINT');
CALL erp_add_column('md_material', 'tax_rate', 'DECIMAL(9,4) NOT NULL DEFAULT 13');
-- 客户：账期、折扣、银行账户
CALL erp_add_column('md_customer', 'payment_days', 'INT NOT NULL DEFAULT 0');
CALL erp_add_column('md_customer', 'discount_rate', 'DECIMAL(9,4) NOT NULL DEFAULT 100');
CALL erp_add_column('md_customer', 'bank_name', 'VARCHAR(100)');
CALL erp_add_column('md_customer', 'bank_account', 'VARCHAR(50)');
CALL erp_add_column('md_customer', 'dept_id', 'BIGINT');
-- 供应商：账期、银行账户
CALL erp_add_column('md_supplier', 'payment_days', 'INT NOT NULL DEFAULT 0');
CALL erp_add_column('md_supplier', 'bank_name', 'VARCHAR(100)');
CALL erp_add_column('md_supplier', 'bank_account', 'VARCHAR(50)');
-- 仓库：是否启用批次/序列号、是否允许负库存
CALL erp_add_column('md_warehouse', 'allow_negative', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('md_warehouse', 'freeze_stock', 'SMALLINT NOT NULL DEFAULT 0');
-- ------------------------------------------------------------
-- 4. 业务单据：数据权限部门字段、成本价税分离、反审核字段
-- ------------------------------------------------------------

CALL erp_add_column('pa_order', 'dept_id', 'BIGINT');
CALL erp_add_column('pa_instock', 'dept_id', 'BIGINT');
CALL erp_add_column('pa_instock', 'tax_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pa_instock', 'total_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pa_return', 'dept_id', 'BIGINT');
CALL erp_add_column('pa_return', 'tax_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pa_return', 'total_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('sa_order', 'dept_id', 'BIGINT');
CALL erp_add_column('sa_outstock', 'dept_id', 'BIGINT');
CALL erp_add_column('sa_outstock', 'tax_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('sa_outstock', 'net_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('sa_outstock', 'total_cost_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('sa_return', 'dept_id', 'BIGINT');
CALL erp_add_column('sa_return', 'tax_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ar_receivable', 'dept_id', 'BIGINT');
CALL erp_add_column('ar_receivable', 'tax_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ar_receivable', 'currency', 'VARCHAR(10)');
CALL erp_add_column('ar_receivable', 'original_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ar_receivable', 'bad_debt_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ar_receipt', 'dept_id', 'BIGINT');
CALL erp_add_column('ap_payable', 'dept_id', 'BIGINT');
CALL erp_add_column('ap_payable', 'tax_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ap_payment', 'dept_id', 'BIGINT');
-- 明细表补审计字段
CALL erp_add_column('pa_order_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pa_order_entry', 'create_by', 'BIGINT');
CALL erp_add_column('pa_order_entry', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pa_order_entry', 'update_by', 'BIGINT');
CALL erp_add_column('pa_order_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pa_instock_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pa_instock_entry', 'create_by', 'BIGINT');
CALL erp_add_column('pa_instock_entry', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pa_instock_entry', 'update_by', 'BIGINT');
CALL erp_add_column('pa_instock_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pa_return_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pa_return_entry', 'create_by', 'BIGINT');
CALL erp_add_column('pa_return_entry', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pa_return_entry', 'update_by', 'BIGINT');
CALL erp_add_column('pa_return_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('sa_order_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('sa_order_entry', 'create_by', 'BIGINT');
CALL erp_add_column('sa_order_entry', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('sa_order_entry', 'update_by', 'BIGINT');
CALL erp_add_column('sa_order_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('sa_outstock_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('sa_outstock_entry', 'create_by', 'BIGINT');
CALL erp_add_column('sa_outstock_entry', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('sa_outstock_entry', 'update_by', 'BIGINT');
CALL erp_add_column('sa_outstock_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('sa_return_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('sa_return_entry', 'create_by', 'BIGINT');
CALL erp_add_column('sa_return_entry', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('sa_return_entry', 'update_by', 'BIGINT');
CALL erp_add_column('sa_return_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('ic_transfer_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('ic_transfer_entry', 'create_by', 'BIGINT');
CALL erp_add_column('ic_transfer_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('ic_check_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('ic_check_entry', 'create_by', 'BIGINT');
CALL erp_add_column('ic_check_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pm_bom_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pm_bom_entry', 'create_by', 'BIGINT');
CALL erp_add_column('pm_bom_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pm_mo_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pm_mo_entry', 'create_by', 'BIGINT');
CALL erp_add_column('pm_mo_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pm_pick_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pm_pick_entry', 'create_by', 'BIGINT');
CALL erp_add_column('pm_pick_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('hr_salary_entry', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('hr_salary_entry', 'create_by', 'BIGINT');
CALL erp_add_column('hr_salary_entry', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');

-- 盘点差异原因码与金额
CALL erp_add_column('ic_check', 'gain_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ic_check', 'loss_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ic_check', 'reason_code', 'VARCHAR(30)');
CALL erp_add_column('ic_check_entry', 'gain_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ic_check_entry', 'loss_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ic_check_entry', 'reason_code', 'VARCHAR(30)');
-- 库存流水补金额，支持从流水重算库存金额
CALL erp_add_column('ic_transaction', 'price', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ic_transaction', 'amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_index('ic_transaction', 'idx_ic_transaction_stock', 'material_id, warehouse_id, batch_no');
-- ------------------------------------------------------------
-- 5. 工作流：条件分支、表达式、动态审批人、转办加签、超时
-- ------------------------------------------------------------

CALL erp_add_column('wf_node', 'condition_expr', 'VARCHAR(500)');
CALL erp_add_column('wf_node', 'node_type_cond', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('wf_node', 'timeout_hours', 'INT NOT NULL DEFAULT 0');
CALL erp_add_column('wf_node', 'default_assignee', 'VARCHAR(200)');
CALL erp_add_column('wf_node', 'multi_approve', 'SMALLINT NOT NULL DEFAULT 1');
CALL erp_add_column('wf_node', 'cc_flag', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('wf_node', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('wf_node', 'deleted', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('wf_task', 'node_key', 'VARCHAR(50)');
CALL erp_add_column('wf_task', 'source_task_id', 'BIGINT');
CALL erp_add_column('wf_task', 'action_type', 'VARCHAR(20)');
CALL erp_add_column('wf_task', 'timeout_at', 'DATETIME');
CALL erp_add_column('wf_task', 'remind_count', 'INT NOT NULL DEFAULT 0');
CALL erp_add_index('wf_task', 'idx_wf_task_node', 'node_id, status');
CREATE TABLE IF NOT EXISTS wf_task_record (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id     BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    node_id     BIGINT NOT NULL,
    action      VARCHAR(20) NOT NULL,
    comment     VARCHAR(500),
    operator_id BIGINT,
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('wf_task_record', 'idx_wf_task_record_task', 'task_id');
CREATE TABLE IF NOT EXISTS wf_variable (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id BIGINT NOT NULL,
    var_key     VARCHAR(50) NOT NULL,
    var_name    VARCHAR(100),
    var_type    VARCHAR(20) NOT NULL DEFAULT 'STRING',
    default_value VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('wf_variable', 'idx_wf_variable_def', 'definition_id');
-- ------------------------------------------------------------
-- 6. 生产制造：工艺路线、报工、成本归集、BOM 版本、MRP 投放
-- ------------------------------------------------------------

CALL erp_add_column('pm_bom', 'base_qty', 'DECIMAL(19,4) NOT NULL DEFAULT 1');
CALL erp_add_column('pm_bom', 'effective_date', 'DATE');
CALL erp_add_column('pm_bom', 'expiry_date', 'DATE');
CALL erp_add_column('pm_bom', 'is_default', 'TINYINT NOT NULL DEFAULT 1');
CALL erp_add_column('pm_bom_entry', 'substitute_flag', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pm_bom_entry', 'substitute_material_id', 'BIGINT');
CALL erp_add_column('pm_bom_entry', 'position_no', 'INT NOT NULL DEFAULT 0');
CREATE TABLE IF NOT EXISTS pm_routing (
    id           BIGINT PRIMARY KEY,
    routing_no   VARCHAR(30) NOT NULL,
    material_id  BIGINT NOT NULL,
    version      VARCHAR(20) NOT NULL DEFAULT 'V1',
    status       SMALLINT NOT NULL DEFAULT 1,
    effective_date DATE,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `pm_routing_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `routing_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pm_routing_no` (`pm_routing_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pm_routing_operation (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    routing_id   BIGINT NOT NULL,
    operation_no VARCHAR(30) NOT NULL,
    operation_name VARCHAR(100) NOT NULL,
    work_center_id BIGINT,
    standard_hours DECIMAL(19,4) NOT NULL DEFAULT 0,
    standard_qty  DECIMAL(19,4) NOT NULL DEFAULT 0,
    labor_rate    DECIMAL(19,4) NOT NULL DEFAULT 0,
    overhead_rate DECIMAL(19,4) NOT NULL DEFAULT 0,
    sort_no      INT NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_routing_operation', 'idx_pm_routing_op_routing', 'routing_id');
-- 补齐 BaseEntity 审计字段
CALL erp_add_column('pm_routing_operation', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL erp_add_column('pm_routing_operation', 'update_by', 'BIGINT');
CREATE TABLE IF NOT EXISTS pm_work_center (
    id           BIGINT PRIMARY KEY,
    center_code  VARCHAR(30) NOT NULL,
    center_name  VARCHAR(100) NOT NULL,
    capacity_hours DECIMAL(19,4) NOT NULL DEFAULT 0,
    status       SMALLINT NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `pm_wc_code_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `center_code` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pm_work_center_code` (`pm_wc_code_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pm_report_work (
    id           BIGINT PRIMARY KEY,
    report_no    VARCHAR(30) NOT NULL,
    mo_id        BIGINT NOT NULL,
    operation_id BIGINT,
    employee_id  BIGINT,
    report_date  DATE NOT NULL,
    qty          DECIMAL(19,4) NOT NULL DEFAULT 0,
    qualified_qty DECIMAL(19,4) NOT NULL DEFAULT 0,
    scrap_qty    DECIMAL(19,4) NOT NULL DEFAULT 0,
    work_hours   DECIMAL(19,4) NOT NULL DEFAULT 0,
    status       SMALLINT NOT NULL DEFAULT 0,
    voucher_id   BIGINT,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `pm_report_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `report_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_pm_report_work_no` (`pm_report_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_report_work', 'idx_pm_report_mo', 'mo_id');
CREATE TABLE IF NOT EXISTS pm_manufacturing_cost (
    id           BIGINT PRIMARY KEY,
    mo_id        BIGINT NOT NULL,
    cost_type    SMALLINT NOT NULL DEFAULT 1,
    cost_date    DATE NOT NULL,
    labor_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    overhead_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    other_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    source_type  VARCHAR(30),
    source_id    BIGINT,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('pm_manufacturing_cost', 'idx_pm_cost_mo', 'mo_id');
-- MRP 结果：建议日期 + 投放状态
CALL erp_add_column('pm_mrp_result', 'suggest_date', 'DATE');
CALL erp_add_column('pm_mrp_result', 'release_status', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pm_mrp_result', 'released_order_id', 'BIGINT');
CALL erp_add_column('pm_mrp_result', 'bom_id', 'BIGINT');
CALL erp_add_column('pm_mrp_result', 'unit_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pm_mrp_result', 'lead_time_days', 'INT NOT NULL DEFAULT 0');
-- 生产订单：委外标记、成本归集状态
CALL erp_add_column('pm_mo', 'dept_id', 'BIGINT');
CALL erp_add_column('pm_mo', 'outsourced', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pm_mo', 'labor_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pm_mo', 'overhead_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pm_mo', 'total_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
-- ------------------------------------------------------------
-- 7. 人力资源：员工薪资属性、社保个税、考勤
-- ------------------------------------------------------------

CALL erp_add_column('hr_employee', 'id_card', 'VARCHAR(30)');
CALL erp_add_column('hr_employee', 'bank_name', 'VARCHAR(100)');
CALL erp_add_column('hr_employee', 'bank_account', 'VARCHAR(50)');
CALL erp_add_column('hr_employee', 'base_salary', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_employee', 'position_salary', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_employee', 'social_base', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_employee', 'fund_base', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_employee', 'user_id', 'BIGINT');
CALL erp_add_column('hr_employee', 'contract_end_date', 'DATE');
CALL erp_add_column('hr_salary', 'dept_id2', 'BIGINT');
CALL erp_add_column('hr_salary', 'gross_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_salary', 'tax_amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_salary', 'social_personal', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_salary', 'social_company', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_salary', 'fund_personal', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_salary', 'fund_company', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CREATE TABLE IF NOT EXISTS hr_attendance (
    id           BIGINT PRIMARY KEY,
    employee_id  BIGINT NOT NULL,
    work_date    DATE NOT NULL,
    shift_code   VARCHAR(30),
    clock_in     DATETIME,
    clock_out    DATETIME,
    work_hours   DECIMAL(9,2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(9,2) NOT NULL DEFAULT 0,
    leave_type   VARCHAR(30),
    leave_hours  DECIMAL(9,2) NOT NULL DEFAULT 0,
    status       SMALLINT NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `hr_attendance_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN CONCAT('|', `employee_id`, '|', `work_date`) ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_hr_attendance` (`hr_attendance_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS hr_social_policy (
    id           BIGINT PRIMARY KEY,
    policy_name  VARCHAR(100) NOT NULL,
    effective_date DATE NOT NULL,
    social_rate  DECIMAL(9,4) NOT NULL DEFAULT 0,
    fund_rate    DECIMAL(9,4) NOT NULL DEFAULT 0,
    tax_threshold DECIMAL(19,4) NOT NULL DEFAULT 5000,
    tax_rate1    DECIMAL(9,4) NOT NULL DEFAULT 3,
    tax_rate2    DECIMAL(9,4) NOT NULL DEFAULT 10,
    tax_rate3    DECIMAL(9,4) NOT NULL DEFAULT 20,
    tax_rate4    DECIMAL(9,4) NOT NULL DEFAULT 25,
    tax_rate5    DECIMAL(9,4) NOT NULL DEFAULT 30,
    tax_rate6    DECIMAL(9,4) NOT NULL DEFAULT 35,
    tax_rate7    DECIMAL(9,4) NOT NULL DEFAULT 45,
    quick_deduction DECIMAL(19,4) NOT NULL DEFAULT 0,
    status       SMALLINT NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 个税速算扣除数：每档一个值，与对应税率一一对应
CALL erp_add_column('hr_social_policy', 'tax_deduction1', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_social_policy', 'tax_deduction2', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_social_policy', 'tax_deduction3', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_social_policy', 'tax_deduction4', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_social_policy', 'tax_deduction5', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_social_policy', 'tax_deduction6', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('hr_social_policy', 'tax_deduction7', 'DECIMAL(19,4) NOT NULL DEFAULT 0');

-- ------------------------------------------------------------
-- 8. 应收应付：坏账、预收预付、往来对账
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ar_advance (
    id           BIGINT PRIMARY KEY,
    advance_no   VARCHAR(30) NOT NULL,
    customer_id  BIGINT NOT NULL,
    advance_date DATE NOT NULL,
    amount       DECIMAL(19,4) NOT NULL DEFAULT 0,
    used_amount  DECIMAL(19,4) NOT NULL DEFAULT 0,
    status       SMALLINT NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `ar_advance_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `advance_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ar_advance_no` (`ar_advance_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ap_advance (
    id           BIGINT PRIMARY KEY,
    advance_no   VARCHAR(30) NOT NULL,
    supplier_id  BIGINT NOT NULL,
    advance_date DATE NOT NULL,
    amount       DECIMAL(19,4) NOT NULL DEFAULT 0,
    used_amount  DECIMAL(19,4) NOT NULL DEFAULT 0,
    status       SMALLINT NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `ap_advance_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `advance_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ap_advance_no` (`ap_advance_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ar_bad_debt (
    id           BIGINT PRIMARY KEY,
    record_no    VARCHAR(30) NOT NULL,
    period       VARCHAR(7) NOT NULL,
    customer_id  BIGINT NOT NULL,
    amount       DECIMAL(19,4) NOT NULL DEFAULT 0,
    reason       VARCHAR(255),
    status       SMALLINT NOT NULL DEFAULT 0,
    voucher_id   BIGINT,
    audit_by     BIGINT,
    audit_time   DATETIME,
    remark       VARCHAR(255),
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
  `ar_bad_debt_no_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `record_no` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_ar_bad_debt_no` (`ar_bad_debt_no_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- 9. 库存：占用量 / 在途量
-- ------------------------------------------------------------

CALL erp_add_column('ic_stock', 'in_transit_qty', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ic_stock', 'amount', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('ic_stock', 'avg_price', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
-- ------------------------------------------------------------
-- 10. 索引优化
-- ------------------------------------------------------------

CALL erp_add_index('fi_voucher_entry', 'idx_fi_voucher_entry_account_period', 'account_id');
CALL erp_add_index('ar_receivable', 'idx_ar_receivable_status_due', 'status, due_date');
CALL erp_add_index('ap_payable', 'idx_ap_payable_status_due', 'status, due_date');
CALL erp_add_index('pm_mo', 'idx_pm_mo_status', 'status');
CALL erp_add_index('sa_order', 'idx_sa_order_status', 'status');
CALL erp_add_index('pa_order', 'idx_pa_order_status', 'status');








