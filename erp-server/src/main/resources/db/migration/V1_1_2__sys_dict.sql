-- ============================================================
-- ERP 管理系统 数据字典与系统参数（V1.1.2）
-- 目的：把散落在代码里的状态枚举、类型选项收敛到可配置的数据字典，
--       业务代码只依赖字典编码，新增选项无需改代码发版。
-- ============================================================
USE erp;

-- ------------------------------------------------------------
-- 1. 数据字典类型
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGINT PRIMARY KEY,
    dict_type   VARCHAR(100) NOT NULL,
    dict_name   VARCHAR(100) NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 0,
    remark      VARCHAR(255),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
  `sys_dict_type_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `dict_type` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_sys_dict_type` (`sys_dict_type_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- 2. 数据字典项
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_type   VARCHAR(100) NOT NULL,
    dict_label  VARCHAR(100) NOT NULL,
    dict_value  VARCHAR(100) NOT NULL,
    sort_no     INT          NOT NULL DEFAULT 0,
    css_class   VARCHAR(50),
    list_class  VARCHAR(50),
    is_default  SMALLINT     NOT NULL DEFAULT 0,
    status      SMALLINT     NOT NULL DEFAULT 0,
    remark      VARCHAR(255),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('sys_dict_data', 'idx_sys_dict_data_type', 'dict_type, sort_no');

-- ------------------------------------------------------------
-- 3. 系统参数
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_param (
    id           BIGINT PRIMARY KEY,
    param_key    VARCHAR(100) NOT NULL,
    param_value  VARCHAR(500),
    param_name   VARCHAR(100),
    param_type   VARCHAR(20) NOT NULL DEFAULT 'STRING',
    status       SMALLINT     NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    BIGINT,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    deleted      SMALLINT     NOT NULL DEFAULT 0,
  `sys_param_key_uniq` VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `param_key` ELSE CONCAT('d', `id`) END) STORED,
  UNIQUE KEY `uk_sys_param_key` (`sys_param_key_uniq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- 4. 字典种子数据：把核心单据状态与类型固化下来
-- ------------------------------------------------------------
INSERT INTO sys_dict_type (id, dict_type, dict_name, remark) VALUES
(99001, 'pa_order_status', '采购订单状态', '采购订单状态机'),
(99002, 'pa_instock_status', '采购入库单状态', NULL),
(99003, 'sa_order_status', '销售订单状态', NULL),
(99004, 'sa_outstock_status', '销售出库单状态', NULL),
(99005, 'fi_voucher_status', '记账凭证状态', NULL),
(99006, 'fi_period_status', '会计期间状态', NULL),
(99007, 'wf_task_status', '工作流任务状态', NULL),
(99008, 'ic_check_status', '库存盘点单状态', NULL),
(99009, 'pm_mo_status', '生产订单状态', NULL),
(99010, 'hr_salary_status', '薪资单状态', NULL),
(99011, 'md_warehouse_type', '仓库类型', NULL),
(99012, 'ar_receivable_status', '应收单状态', NULL),
(99013, 'ap_payable_status', '应付单状态', NULL),
(99014, 'common_yes_no', '是否', '通用是否选项'),
(99015, 'pm_procure_type', '物料采购类型', '1 采购 2 自制 3 委外')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), remark = VALUES(remark);

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_no, list_class, is_default) VALUES
('pa_order_status', '暂存', '0', 1, 'info', 1),
('pa_order_status', '已审核', '1', 2, 'warning', 0),
('pa_order_status', '已完成', '2', 3, 'success', 0),
('pa_order_status', '已关闭', '3', 4, 'danger', 0),
('pa_instock_status', '暂存', '0', 1, 'info', 1),
('pa_instock_status', '已审核', '1', 2, 'success', 0),
('sa_order_status', '暂存', '0', 1, 'info', 1),
('sa_order_status', '已审核', '1', 2, 'warning', 0),
('sa_order_status', '已完成', '2', 3, 'success', 0),
('sa_order_status', '已关闭', '3', 4, 'danger', 0),
('sa_outstock_status', '暂存', '0', 1, 'info', 1),
('sa_outstock_status', '已审核', '1', 2, 'success', 0),
('fi_voucher_status', '暂存', '0', 1, 'info', 1),
('fi_voucher_status', '已提交', '1', 2, 'primary', 0),
('fi_voucher_status', '已审核', '2', 3, 'warning', 0),
('fi_voucher_status', '已记账', '3', 4, 'success', 0),
('fi_voucher_status', '已红冲', '4', 5, 'danger', 0),
('fi_voucher_status', '已作废', '5', 6, 'danger', 0),
('fi_period_status', '未启用', '0', 1, 'info', 0),
('fi_period_status', '已启用', '1', 2, 'success', 1),
('fi_period_status', '已结账', '2', 3, 'danger', 0),
('wf_task_status', '待审批', '0', 1, 'warning', 1),
('wf_task_status', '已通过', '1', 2, 'success', 0),
('wf_task_status', '已驳回', '2', 3, 'danger', 0),
('wf_task_status', '已取消', '3', 4, 'info', 0),
('wf_task_status', '已转办', '4', 5, 'info', 0),
('wf_task_status', '已加签', '5', 6, 'info', 0),
('wf_task_status', '等待前置任务', '6', 7, 'info', 0),
('ic_check_status', '暂存', '0', 1, 'info', 1),
('ic_check_status', '已审核', '1', 2, 'success', 0),
('pm_mo_status', '暂存', '0', 1, 'info', 1),
('pm_mo_status', '已下达', '1', 2, 'warning', 0),
('pm_mo_status', '生产中', '2', 3, 'primary', 0),
('pm_mo_status', '已完工', '3', 4, 'success', 0),
('pm_mo_status', '已关闭', '4', 5, 'danger', 0),
('hr_salary_status', '暂存', '0', 1, 'info', 1),
('hr_salary_status', '已审核', '1', 2, 'success', 0),
('md_warehouse_type', '实仓', '1', 1, 'success', 1),
('md_warehouse_type', '不良品仓', '2', 2, 'danger', 0),
('md_warehouse_type', '在制品仓', '3', 3, 'warning', 0),
('md_warehouse_type', '待检仓', '4', 4, 'warning', 0),
('ar_receivable_status', '未结清', '0', 1, 'warning', 1),
('ar_receivable_status', '已结清', '1', 2, 'success', 0),
('ap_payable_status', '未结清', '0', 1, 'warning', 1),
('ap_payable_status', '已结清', '1', 2, 'success', 0),
('common_yes_no', '是', '1', 1, 'success', 1),
('common_yes_no', '否', '0', 2, 'info', 0),
('pm_procure_type', '采购', '1', 1, 'primary', 1),
('pm_procure_type', '自制', '2', 2, 'success', 0),
('pm_procure_type', '委外', '3', 3, 'warning', 0);

INSERT INTO sys_param (id, param_key, param_value, param_name, param_type, remark) VALUES
(99101, 'erp.name', '示例科技有限公司', '企业名称', 'STRING', '用于单据打印与报表抬头'),
(99102, 'erp.default_tax_rate', '13', '默认增值税率', 'NUMBER', '新建业务单据时的默认税率'),
(99103, 'erp.stock_warning_enabled', 'true', '启用库存预警', 'BOOLEAN', '低于安全库存时在驾驶舱提示'),
(99104, 'erp.credit_control_enabled', 'true', '启用信用额度控制', 'BOOLEAN', '销售下单时校验客户信用额度'),
(99105, 'erp.voucher_require_separation', 'true', '凭证制单审核分离', 'BOOLEAN', '制单人不能审核自己的凭证')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value), param_name = VALUES(param_name),
    param_type = VALUES(param_type), remark = VALUES(remark);








