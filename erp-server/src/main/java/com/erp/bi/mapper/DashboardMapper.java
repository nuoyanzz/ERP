package com.erp.bi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工作台统计查询。所有指标均来自真实业务数据的聚合。
 */
@Mapper
public interface DashboardMapper {

    /**
     * 本期销售出库金额（价税合计）。
     */
    @Select("""
            SELECT COALESCE(SUM(total_amount), 0) AS amount
            FROM sa_outstock
            WHERE deleted = 0 AND status = 1 AND outstock_date BETWEEN #{startDate} AND #{endDate}
            """)
    BigDecimal sumSalesAmount(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 本期采购入库金额。
     */
    @Select("""
            SELECT COALESCE(SUM(total_amount), 0) AS amount
            FROM pa_instock
            WHERE deleted = 0 AND status = 1 AND instock_date BETWEEN #{startDate} AND #{endDate}
            """)
    BigDecimal sumPurchaseAmount(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 库存总金额 = 数量 × 加权平均价。
     */
    @Select("""
            SELECT COALESCE(SUM(s.qty * COALESCE(m.cost_price, 0)), 0) AS amount
            FROM ic_stock s
            INNER JOIN md_material m ON m.id = s.material_id AND m.deleted = 0
            WHERE s.deleted = 0
            """)
    BigDecimal sumStockAmount();

    /**
     * 应收账款余额（未结清部分）。
     */
    @Select("""
            SELECT COALESCE(SUM(amount - received_amount), 0) AS amount
            FROM ar_receivable
            WHERE deleted = 0 AND status = 0
            """)
    BigDecimal sumReceivableAmount();

    /**
     * 应付账款余额（未结清部分）。
     */
    @Select("""
            SELECT COALESCE(SUM(amount - paid_amount), 0) AS amount
            FROM ap_payable
            WHERE deleted = 0 AND status = 0
            """)
    BigDecimal sumPayableAmount();

    /**
     * 待审核的业务单据数。
     */
    @Select("""
            SELECT (
                (SELECT COUNT(*) FROM sa_outstock WHERE deleted = 0 AND status = 0) +
                (SELECT COUNT(*) FROM pa_instock WHERE deleted = 0 AND status = 0) +
                (SELECT COUNT(*) FROM pm_mo WHERE deleted = 0 AND status = 0) +
                (SELECT COUNT(*) FROM hr_salary WHERE deleted = 0 AND status = 0) +
                (SELECT COUNT(*) FROM fi_voucher WHERE deleted = 0 AND status IN (0, 1))
            ) AS count
            """)
    Integer countPendingAudit();

    /**
     * 当前用户的待办任务数。
     */
    @Select("""
            SELECT COUNT(*) FROM wf_task
            WHERE deleted = 0 AND status = 0 AND assignee_id = #{userId}
            """)
    Integer countTodoTasks(@Param("userId") Long userId);

    /**
     * 库存预警物料数：当前库存低于安全库存的物料种类。
     */
    @Select("""
            SELECT COUNT(*) FROM (
                SELECT m.id
                FROM md_material m
                LEFT JOIN (
                    SELECT material_id, SUM(qty) AS total_qty
                    FROM ic_stock WHERE deleted = 0 GROUP BY material_id
                ) s ON s.material_id = m.id
                WHERE m.deleted = 0 AND m.status = 0 AND m.safe_stock > 0
                  AND COALESCE(s.total_qty, 0) < m.safe_stock
            ) t
            """)
    Integer countStockWarning();

    /**
     * 逾期应收笔数。
     */
    @Select("""
            SELECT COUNT(*) FROM ar_receivable
            WHERE deleted = 0 AND status = 0 AND due_date IS NOT NULL AND due_date < #{asOfDate}
            """)
    Integer countOverdueReceivable(@Param("asOfDate") String asOfDate);

    /**
     * 在制工单数：已下达未完工。
     */
    @Select("""
            SELECT COUNT(*) FROM pm_mo
            WHERE deleted = 0 AND status = 1 AND produced_qty < qty
            """)
    Integer countWipMo();

    /**
     * 销售趋势：本年各月的出库金额。
     */
    @Select("""
            SELECT DATE_FORMAT(outstock_date, '%Y-%m') AS label,
                   COALESCE(SUM(total_amount), 0) AS amount
            FROM sa_outstock
            WHERE deleted = 0 AND status = 1 AND YEAR(outstock_date) = #{year}
            GROUP BY DATE_FORMAT(outstock_date, '%Y-%m')
            ORDER BY label
            """)
    List<Map<String, Object>> salesTrendByMonth(@Param("year") int year);

    /**
     * 库存金额 TOP 物料。
     */
    @Select("""
            SELECT m.material_name AS name,
                   COALESCE(SUM(s.qty * m.cost_price), 0) AS amount,
                   CONCAT(COALESCE(SUM(s.qty), 0), ' ', COALESCE(m.unit, '')) AS extra
            FROM ic_stock s
            INNER JOIN md_material m ON m.id = s.material_id AND m.deleted = 0
            WHERE s.deleted = 0
            GROUP BY m.id, m.material_name, m.unit
            ORDER BY amount DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> stockTop(@Param("limit") int limit);

    /**
     * 逾期应收 TOP 客户。
     */
    @Select("""
            SELECT c.customer_name AS name,
                   COALESCE(SUM(r.amount - r.received_amount), 0) AS amount,
                   CONCAT(COUNT(*), ' 笔逾期') AS extra
            FROM ar_receivable r
            INNER JOIN md_customer c ON c.id = r.customer_id AND c.deleted = 0
            WHERE r.deleted = 0 AND r.status = 0
              AND r.due_date IS NOT NULL AND r.due_date < #{asOfDate}
            GROUP BY c.id, c.customer_name
            ORDER BY amount DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> overdueTop(@Param("asOfDate") String asOfDate, @Param("limit") int limit);
}








