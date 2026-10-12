package com.erp.hr.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrEmployeeQueryRequest extends PageQuery {

    private String employeeNo;
    private String employeeName;
    private Long deptId;
    private Integer status;
}








