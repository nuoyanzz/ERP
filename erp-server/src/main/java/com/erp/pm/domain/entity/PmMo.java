package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_mo")
public class PmMo extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;
    public static final int STATUS_PROCESSING = 2;
    public static final int STATUS_FINISHED = 3;
    public static final int STATUS_CLOSED = 4;

    private String moNo;
    private Long materialId;
    private Long bomId;
    private BigDecimal qty;
    private BigDecimal producedQty;
    private Long warehouseId;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private Integer status;

    /** 生产部门：用于数据权限与成本归集 */
    private Long deptId;

    /** 是否委外：委外订单不参与自制件的人工/制造费用归集 */
    private Integer outsourced;

    /** 累计归集人工成本 */
    private BigDecimal laborCost;

    /** 累计归集制造费用 */
    private BigDecimal overheadCost;

    /** 累计总成本（材料 + 人工 + 制造费用） */
    private BigDecimal totalCost;

    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
}








