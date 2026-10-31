package com.erp.bi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工作台统计数据。全部来自真实业务数据聚合。
 */
@Data
public class DashboardVO {

    /** 统计区间描述，如「本月」 */
    private String periodLabel;

    /** 本期营业收入 */
    private BigDecimal salesAmount;
    /** 本期采购金额 */
    private BigDecimal purchaseAmount;
    /** 库存总金额 */
    private BigDecimal stockAmount;
    /** 应收账款余额 */
    private BigDecimal receivableAmount;
    /** 应付账款余额 */
    private BigDecimal payableAmount;

    /** 待审批单据数（待审核的业务单据） */
    private Integer pendingAuditCount;
    /** 待办任务数（工作流待办） */
    private Integer todoTaskCount;
    /** 库存预警物料数（低于安全库存） */
    private Integer stockWarningCount;
    /** 逾期应收笔数 */
    private Integer overdueReceivableCount;
    /** 在制工单数 */
    private Integer wipMoCount;

    /** 销售趋势（按月，本年） */
    private List<TrendPointVO> salesTrend;
    /** 库存 TOP 物料 */
    private List<RankItemVO> stockTop;
    /** 逾期应收 TOP 客户 */
    private List<RankItemVO> overdueTop;
    /** 待办事项 */
    private List<TodoItemVO> todoItems;

    /**
     * 趋势点。
     */
    @Data
    public static class TrendPointVO {
        private String label;
        private BigDecimal amount;
    }

    /**
     * 排行项。
     */
    @Data
    public static class RankItemVO {
        private String name;
        private BigDecimal amount;
        private String extra;
    }

    /**
     * 待办事项。
     */
    @Data
    public static class TodoItemVO {
        private String title;
        private String type;
        private Integer count;
        private String route;
    }
}








