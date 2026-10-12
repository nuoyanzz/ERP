package com.erp.fi.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FiPeriodSaveRequest {

    @NotNull(message = "年度不能为空")
    @Min(value = 2000, message = "年度不合法")
    @Max(value = 2999, message = "年度不合法")
    private Integer periodYear;

    @NotNull(message = "期间号不能为空")
    @Min(value = 1, message = "期间号取值1-12")
    @Max(value = 12, message = "期间号取值1-12")
    private Integer periodNo;

    private String remark;
}








