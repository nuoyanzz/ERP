package com.erp.hr.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceQueryRequest extends PageQuery {

    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
}
