-- ============================================================
-- ERP 管理系统 数据库增量迁移脚本 V1.1.1
-- 目标：补齐工作流引擎 V1.1.0 之后新增的字段与表
-- 说明：全部使用 CALL erp_add_column / CREATE TABLE IF NOT EXISTS
--       可重复执行，兼容 MySQL 8.0
-- ============================================================
USE erp;

-- ------------------------------------------------------------
-- 1. 流程实例：挂起原因与退回次数
--    挂起用于「解析不到审批人」时记录原因，退回次数用于防止退回死循环
-- ------------------------------------------------------------

CALL erp_add_column('wf_instance', 'suspend_reason', 'VARCHAR(500)');
CALL erp_add_column('wf_instance', 'rollback_count', 'INT NOT NULL DEFAULT 0');
CALL erp_add_index('wf_instance', 'idx_wf_instance_status', 'biz_type, biz_id, status');

-- ------------------------------------------------------------
-- 2. 实例级流程变量表
--    流程发起时的单据关键字段（金额、部门、类型等）需要跨请求持久化，
--    否则后续节点的条件表达式无法再求值
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS wf_instance_variable (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id BIGINT       NOT NULL,
    var_key     VARCHAR(50)  NOT NULL,
    var_value   VARCHAR(500),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL erp_add_index('wf_instance_variable', 'idx_wf_instance_variable_instance', 'instance_id');

-- ------------------------------------------------------------
-- 3. 流程定义：按业务类型索引
--    同一业务类型可能存在多个流程定义，发起时按版本倒序取已发布的一个
-- ------------------------------------------------------------

CALL erp_add_index('wf_definition', 'idx_wf_definition_biz_type', 'biz_type, status');








