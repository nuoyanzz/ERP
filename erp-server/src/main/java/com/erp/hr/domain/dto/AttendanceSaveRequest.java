package com.erp.hr.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤录入请求。
 */
@Data
public class AttendanceSaveRequest {

    private Long id;

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @NotNull(message = "考勤日期不能为空")
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
