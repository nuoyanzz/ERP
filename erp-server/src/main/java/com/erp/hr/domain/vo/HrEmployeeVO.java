package com.erp.hr.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HrEmployeeVO {

    private Long id;
    private String employeeNo;
    private String employeeName;
    private Integer gender;
    private Long deptId;
    private String deptName;
    private String position;
    private LocalDate entryDate;
    private LocalDate leaveDate;
    private String phone;
    private String email;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}








