package com.erp.hr.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HrSalarySaveRequest {

    private Long id;

    @NotBlank(message = "薪资期间不能为空")
    @Size(max = 7, message = "薪资期间格式不合法")
    private String period;

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "薪资项目不能为空")
    private List<HrSalaryEntryRequest> entries;
}








