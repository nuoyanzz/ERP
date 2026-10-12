package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary")
public class HrSalary extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    public static final int ITEM_BASE = 1;
    public static final int ITEM_MERIT = 2;
    public static final int ITEM_ALLOWANCE = 3;
    public static final int ITEM_DEDUCT = 4;
    public static final int ITEM_SOCIAL = 5;

    private String salaryNo;
    private String period;
    private Long employeeId;
    private Long deptId;
    private BigDecimal baseAmount;
    private BigDecimal meritAmount;
    private BigDecimal allowanceAmount;
    private BigDecimal deductAmount;
    private BigDecimal socialAmount;
    private BigDecimal netAmount;

    /** 应发总额 = 基本工资 + 绩效 + 补贴 */
    private BigDecimal grossAmount;
    /** 代扣个人所得税 */
    private BigDecimal taxAmount;
    /** 代扣社保公积金（个人部分） */
    private BigDecimal socialPersonal;
    /** 社保公积金（单位部分），形成企业成本 */
    private BigDecimal socialCompany;

    private Integer status;
    private Long voucherId;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
}








