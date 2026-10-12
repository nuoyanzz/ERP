package com.erp.hr.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HrSalaryEntryRequest {

    @NotNull(message = "项目类型不能为空")
    @Min(value = 1, message = "项目类型取值1-5")
    @Max(value = 5, message = "项目类型取值1-5")
    private Integer itemType;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 50, message = "项目名称长度不能超过50")
    private String itemName;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能小于0")
    private BigDecimal amount;
}








