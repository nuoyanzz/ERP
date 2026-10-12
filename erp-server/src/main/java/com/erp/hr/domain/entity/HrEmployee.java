package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}








