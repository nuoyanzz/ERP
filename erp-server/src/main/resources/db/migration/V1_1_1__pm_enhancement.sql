-- ============================================================
-- ERP 管理系统 数据库增量迁移脚本 V1.1.1
-- 目标：生产制造模块（pm）增强
--   1. 完工入库成本结转拆分（材料 / 人工 / 制造费用）
--   2. 领料单支持替代料
--   3. 凭证科目规则（PM_REPORT 报工、PM_FINISH 拆分）
--   4. 会计科目补齐（221101/221102、500101 明细化）
-- 说明：全部使用 erp_add_column / erp_add_index 辅助过程，可重复执行
-- ============================================================
USE erp;

-- ------------------------------------------------------------
-- 1. 完工入库单：拆分材料成本与人工/制造费用
--    分批完工时按累计完工产量比例结转，必须分别记录各部分，
--    否则无法计算「本次应结转 = 累计应结转 − 之前已结转」的差额。
-- ------------------------------------------------------------
CALL erp_add_column('pm_finish', 'material_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pm_finish', 'labor_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pm_finish', 'overhead_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pm_finish', 'unit_cost', 'DECIMAL(19,4) NOT NULL DEFAULT 0');
CALL erp_add_column('pm_finish', 'cost_voucher_id', 'BIGINT');

-- ------------------------------------------------------------
-- 2. 领料单明细：支持替代料
--    substitute_flag=1 时，实际领用 substitute_material_id 指定的物料，
--    成本按替代料的成本价计算。
-- ------------------------------------------------------------
CALL erp_add_column('pm_pick_entry', 'substitute_flag', 'SMALLINT NOT NULL DEFAULT 0');
CALL erp_add_column('pm_pick_entry', 'substitute_material_id', 'BIGINT');
CALL erp_add_column('pm_pick_entry', 'original_material_id', 'BIGINT');

-- ------------------------------------------------------------
-- 2b. MRP 结果：按批量取整后的计划下单数量
--     净需求是理论值，实际下单量还要按 lot_size / min_order_qty 向上取整，
--     投放单据时用的是这个取整后的数量。
-- ------------------------------------------------------------
CALL erp_add_column('pm_mrp_result', 'planned_qty', 'DECIMAL(19,4) NOT NULL DEFAULT 0');

-- ------------------------------------------------------------
-- 3. BOM：同一物料同一版本唯一、默认版本唯一
--    唯一索引放在生成列上，软删除后自动释放。
-- ------------------------------------------------------------
CALL erp_add_unique_index('pm_bom', 'uk_pm_bom_material_version', 'material_id, version');
CALL erp_add_index('pm_bom', 'idx_pm_bom_default', 'material_id, is_default');

-- ------------------------------------------------------------
-- 4. 会计科目补齐与明细化
--    生成凭证只能记明细科目（FiVoucherServiceImpl 会校验 is_leaf）。
--    现场库中 5001 生产成本 is_leaf=0、2211 应付职工薪酬缺少子科目，
--    这里补齐明细科目并修正父子关系，保证 PM 凭证可以正常生成。
-- ------------------------------------------------------------
INSERT INTO fi_account (id, subject_code, subject_name, parent_id, category, balance_dir, status, is_leaf, carry_forward, cash_flow_type, quantity_account, unit)
VALUES (91101, '221101', '应付职工薪酬-工资', 0, 2, 2, 0, 1, 1, 0, 0, NULL),
       (91102, '221102', '应付职工薪酬-社会保险费', 0, 2, 2, 0, 1, 1, 0, 0, NULL)
ON DUPLICATE KEY UPDATE subject_name = VALUES(subject_name), status = 0, is_leaf = 1,
    category = VALUES(category), balance_dir = VALUES(balance_dir), deleted = 0;

-- 应付职工薪酬-工资/社保：挂到 2211 应付职工薪酬父科目下，保持科目树完整
UPDATE fi_account SET parent_id = (SELECT id FROM (SELECT id FROM fi_account WHERE subject_code = '2211' AND deleted = 0 LIMIT 1) t)
WHERE subject_code IN ('221101', '221102') AND deleted = 0
  AND EXISTS (SELECT 1 FROM (SELECT id FROM fi_account WHERE subject_code = '2211' AND deleted = 0 LIMIT 1) p);

-- 生产成本：父科目 5001 保持非明细，明细挂到 500101/500102 下，
-- 并把 500101/500102 的 parent_id 修正为真实存在的 5001 主键。
UPDATE fi_account SET parent_id = (SELECT id FROM (SELECT id FROM fi_account WHERE subject_code = '5001' AND deleted = 0 LIMIT 1) t)
WHERE subject_code IN ('500101', '500102') AND deleted = 0
  AND (parent_id = 0 OR parent_id IS NULL
       OR parent_id NOT IN (SELECT id FROM (SELECT id FROM fi_account WHERE deleted = 0) x));

-- 5001 已有明细子科目，必须保持 is_leaf=0；确认子科目存在后才置 0
UPDATE fi_account SET is_leaf = 0
WHERE subject_code = '5001' AND deleted = 0
  AND EXISTS (SELECT 1 FROM (SELECT id FROM fi_account WHERE deleted = 0 AND parent_id = fi_account.id) c);

-- ------------------------------------------------------------
-- 5. 凭证科目规则
--    PM_REPORT：报工归集人工成本 —— 借 生产成本(500101)，贷 应付职工薪酬(221101)
--               制造费用单独归集时借 生产成本(500101)，贷 制造费用(5101)
--    PM_FINISH：完工入库 —— 借 库存商品(1405)，贷 生产成本(500101)
-- ------------------------------------------------------------
INSERT INTO fi_biz_account_rule (id, biz_type, rule_key, subject_code, debit_subject_code, credit_subject_code, amount_source, cash_flow_type, remark)
VALUES (95101, 'PM_REPORT', 'COST', '500101', '500101', NULL, 'COST', 0, '生产报工-借方：生产成本-基本生产成本'),
       (95102, 'PM_REPORT', 'PAYABLE', '221101', NULL, '221101', 'COST', 0, '生产报工-贷方：应付职工薪酬-工资'),
       (95103, 'PM_REPORT', 'OVERHEAD', '5101', NULL, '5101', 'COST', 0, '生产报工-贷方：制造费用'),
       (95104, 'PM_FINISH', 'STOCK', '1405', '1405', NULL, 'COST', 0, '完工入库-借方：库存商品'),
       (95105, 'PM_FINISH', 'COST', '500101', NULL, '500101', 'COST', 0, '完工入库-贷方：生产成本-基本生产成本')
ON DUPLICATE KEY UPDATE subject_code = VALUES(subject_code), debit_subject_code = VALUES(debit_subject_code),
    credit_subject_code = VALUES(credit_subject_code), amount_source = VALUES(amount_source),
    cash_flow_type = VALUES(cash_flow_type), remark = VALUES(remark), deleted = 0;

-- 关键修复：存量规则 PM_PICK/PM_FINISH 的 COST 指向 5001「生产成本」父科目，
-- 而 5001 有明细子科目 is_leaf=0，FiVoucherServiceImpl 会直接拒绝记账
-- （"非明细科目不能记账：5001"），导致生产领料与完工入库无法生成凭证。
-- 这里统一改到明细科目 500101「生产成本-基本生产成本」。
UPDATE fi_biz_account_rule
SET subject_code = '500101',
    debit_subject_code = IF(biz_type = 'PM_PICK', '500101', debit_subject_code),
    credit_subject_code = IF(biz_type = 'PM_FINISH', '500101', credit_subject_code),
    update_time = CURRENT_TIMESTAMP
WHERE biz_type IN ('PM_PICK', 'PM_FINISH')
  AND rule_key = 'COST'
  AND subject_code = '5001';

-- ------------------------------------------------------------
-- 6. 菜单权限：新增报工删除、生产成本编辑权限
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, perms, path, component, icon, sort_no, visible)
VALUES (9772, 9707, '报工单删除', 3, 'pm:report:remove', NULL, NULL, NULL, 3, 0),
       (9773, 9707, '报工单修改', 3, 'pm:report:edit', NULL, NULL, NULL, 4, 0),
       (9774, 9708, '成本归集导出', 3, 'pm:cost:export', NULL, NULL, NULL, 1, 0),
       (9763, 9706, '工艺路线审核', 3, 'pm:routing:audit', NULL, NULL, NULL, 4, 0)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms),
    parent_id = VALUES(parent_id), deleted = 0;

-- 生产计划员（角色 7）与超级管理员（角色 1）授予新增权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 7, id FROM sys_menu WHERE id IN (9763, 9772, 9773, 9774) AND deleted = 0;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (9763, 9772, 9773, 9774) AND deleted = 0;








