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
@TableName("pm_finish")
public class PmFinish extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    private String finishNo;
    private Long moId;
    private Long warehouseId;
    private LocalDate finishDate;
    private BigDecimal qty;

    /** 本次结转的总成本（材料 + 人工 + 制造费用） */
    private BigDecimal costAmount;

    /** 本次结转的材料成本 */
    private BigDecimal materialCost;

    /** 本次结转的人工成本 */
    private BigDecimal laborCost;

    /** 本次结转的制造费用 */
    private BigDecimal overheadCost;

    /** 本次结转的单位成本 */
    private BigDecimal unitCost;

    private Integer status;

    /** 完工入库凭证 */
    private Long voucherId;

    /** 成本结转凭证（材料成本与人工制造费用分两笔时记录主凭证） */
    private Long costVoucherId;

    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;

    /** 所属部门，数据权限按此字段过滤 */
    private Long deptId;
}








