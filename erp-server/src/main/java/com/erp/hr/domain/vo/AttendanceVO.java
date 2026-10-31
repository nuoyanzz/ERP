package com.erp.hr.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤展示对象。
 */
@Data
public class AttendanceVO {

    private Long id;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String deptName;
    private LocalDate workDate;
    private String shiftCode;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private BigDecimal workHours;
    private BigDecimal overtimeHours;
    private String leaveType;
    private BigDecimal leaveHours;
    private Integer status;
    private String remark;
}
