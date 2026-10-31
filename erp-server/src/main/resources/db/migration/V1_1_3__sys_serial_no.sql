-- ============================================================
-- 业务单号流水表（V1.1.3）
-- 目的：把「查最大号 + 1」换成数据库原子自增，消除三个真实缺陷：
--   1. 并发下单时两个事务取到同一个号，插入撞唯一键；
--   2. 单据软删除后重新取号会重用旧号，审计链条断裂；
--   3. 单号查询受数据权限裁剪，不同部门取到相同序号。
-- 计数器按「前缀+期间」维度独立自增，与数据权限无关。
-- ============================================================
USE erp;

CREATE TABLE IF NOT EXISTS sys_serial_no (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `prefix`    VARCHAR(50)  NOT NULL,
    `period`    VARCHAR(20)  NOT NULL,
    curr_seq    BIGINT       NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_sys_serial_no` (`prefix`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 历史单号回填：把现有各单据表已用到的最大序号写入流水表，
-- 避免上线后新单据从 0001 重新开始与历史数据撞号。
INSERT INTO sys_serial_no (`prefix`, `period`, curr_seq)
SELECT s.`prefix`, s.period, MAX(s.curr_seq)
FROM (
    SELECT 'CG' AS `prefix`, SUBSTRING_INDEX(order_no, '-', 1) AS period,
           CAST(SUBSTRING_INDEX(order_no, '-', -1) AS UNSIGNED) AS curr_seq
      FROM pa_order WHERE order_no LIKE 'CG-%' AND deleted = 0
    UNION ALL
    SELECT 'RK', SUBSTRING_INDEX(order_no, '-', 1),
           CAST(SUBSTRING_INDEX(order_no, '-', -1) AS UNSIGNED)
      FROM sa_order WHERE order_no LIKE 'RK-%' AND deleted = 0
    UNION ALL
    SELECT 'RK', SUBSTRING_INDEX(instock_no, '-', 1),
           CAST(SUBSTRING_INDEX(instock_no, '-', -1) AS UNSIGNED)
      FROM pa_instock WHERE instock_no LIKE 'RK-%' AND deleted = 0
    UNION ALL
    SELECT 'CK', SUBSTRING_INDEX(outstock_no, '-', 1),
           CAST(SUBSTRING_INDEX(outstock_no, '-', -1) AS UNSIGNED)
      FROM sa_outstock WHERE outstock_no LIKE 'CK-%' AND deleted = 0
) s
GROUP BY s.`prefix`, s.period
ON DUPLICATE KEY UPDATE curr_seq = GREATEST(curr_seq, VALUES(curr_seq));
