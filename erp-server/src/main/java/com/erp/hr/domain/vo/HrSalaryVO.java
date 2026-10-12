package com.erp.hr.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HrSalaryVO {

    private Long id;
    private String salaryNo;
    private String period;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private Long deptId;
    private String deptName;
    private BigDecimal baseAmount;
    private BigDecimal meritAmount;
    private BigDecimal allowanceAmount;
    private BigDecimal deductAmount;
    private BigDecimal socialAmount;
    private BigDecimal netAmount;
    private Integer status;
    private Long voucherId;
    private LocalDateTime auditTime;
    private String remark;
    private LocalDateTime createTime;
    private List<HrSalaryEntryVO> entries;
}








