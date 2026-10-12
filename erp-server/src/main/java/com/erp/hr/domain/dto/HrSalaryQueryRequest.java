package com.erp.hr.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrSalaryQueryRequest extends PageQuery {

    private String period;
    private Long employeeId;
    private Integer status;
}








