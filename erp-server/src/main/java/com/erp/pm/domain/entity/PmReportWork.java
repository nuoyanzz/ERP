package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产报工单：工人/班组对某工单某工序的产出与工时申报。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_report_work")
public class PmReportWork extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    private String reportNo;
    private Long moId;

    /** 工序（来自 pm_routing_operation） */
    private Long operationId;

    /** 报工人 */
    private Long employeeId;

    private LocalDate reportDate;

    /** 报工数量（含合格与报废） */
    private BigDecimal qty;

    /** 合格数量 */
    private BigDecimal qualifiedQty;

    /** 报废数量 */
    private BigDecimal scrapQty;

    /** 工时（小时），用于计算人工成本 */
    private BigDecimal workHours;

    private Integer status;

    /** 审核生成的凭证 */
    private Long voucherId;

    private String remark;
}








