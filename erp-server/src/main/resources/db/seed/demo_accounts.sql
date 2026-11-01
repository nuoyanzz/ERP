-- ============================================================
-- 演示账号（可选导入）
--
-- 本文件用于本地演示与自动化验证，不属于正式初始化流程。
-- 主初始化脚本 data.sql 不包含这些账号——公开仓库内置统一口令的
-- 账号等于把可用权限交给每一个克隆者。
--
-- 导入方式：
--   mysql -uroot -p erp < src/main/resources/db/seed/demo_accounts.sql
--
-- 账号与口令：
--   finance / purchaser / seller / warehouse / hr / producer
--   口令统一为 admin123（BCrypt 密文，仅供本地演示与自动化验证使用）
--
-- 正式部署时请勿导入本文件；管理员账号由 ERP_ADMIN_PASSWORD 环境变量指定。
-- ============================================================
USE erp;

INSERT INTO sys_user (id, username, password, nickname, dept_id, status, remark) VALUES
(1001, 'finance', '$2a$10$dKrHswbcuE4GEsZVgRlAg.x2dneV1iSUedEN3XWwQ9lRMw20RPtoK', '周财务', 96012, 0, '财务主管，财务模块全部权限'),
(1002, 'purchaser', '$2a$10$dKrHswbcuE4GEsZVgRlAg.x2dneV1iSUedEN3XWwQ9lRMw20RPtoK', '采购员小李', 96013, 0, '采购员，本部门数据范围'),
(1003, 'seller', '$2a$10$dKrHswbcuE4GEsZVgRlAg.x2dneV1iSUedEN3XWwQ9lRMw20RPtoK', '销售员小王', 96014, 0, '销售员，本部门数据范围'),
(1004, 'warehouse', '$2a$10$dKrHswbcuE4GEsZVgRlAg.x2dneV1iSUedEN3XWwQ9lRMw20RPtoK', '仓管小陈', 96017, 0, '仓管员，本部门及以下数据范围'),
(1005, 'hr', '$2a$10$dKrHswbcuE4GEsZVgRlAg.x2dneV1iSUedEN3XWwQ9lRMw20RPtoK', '人事小赵', 96016, 0, '人事专员，人力资源模块权限'),
(1006, 'producer', '$2a$10$dKrHswbcuE4GEsZVgRlAg.x2dneV1iSUedEN3XWwQ9lRMw20RPtoK', '生产计划小刘', 96015, 0, '生产计划员，本部门及以下数据范围')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), dept_id = VALUES(dept_id),
    status = VALUES(status), remark = VALUES(remark);

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1001, 2),
(1002, 3),
(1003, 4),
(1004, 5),
(1005, 6),
(1006, 7);
