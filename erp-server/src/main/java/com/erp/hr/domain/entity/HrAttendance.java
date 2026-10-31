package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance")
public class HrAttendance extends BaseEntity {

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_LEAVE = 1;

    private Long employeeId;
    private LocalDate workDate;
    /** 班次编码 */
    private String shiftCode;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    /** 工作时长（小时） */
    private BigDecimal workHours;
    /** 加班时长（小时） */
    private BigDecimal overtimeHours;
    /** 请假类型 */
    private String leaveType;
    /** 请假时长（小时） */
    private BigDecimal leaveHours;
    private Integer status;
    private String remark;
}
