package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class HrEmployee extends BaseEntity {

    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_LEFT = 1;

    private String employeeNo;
    private String employeeName;
    private Integer gender;
    private Long deptId;
    private String position;
    private LocalDate entryDate;
    private LocalDate leaveDate;
    private String phone;
    private String email;
    private Integer status;
    private String remark;

    /** 身份证号 */
    private String idCard;
    /** 开户银行 */
    private String bankName;
    /** 银行账号 */
    private String bankAccount;
    /** 基本工资 */
    private BigDecimal baseSalary;
    /** 岗位工资 */
    private BigDecimal positionSalary;
    /** 社保缴纳基数 */
    private BigDecimal socialBase;
    /** 公积金缴纳基数 */
    private BigDecimal fundBase;
    /** 关联的系统用户 */
    private Long userId;
    /** 劳动合同到期日 */
    private LocalDate contractEndDate;
}
