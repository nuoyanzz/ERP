package com.erp.md.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MdUnitConvertSaveRequest {

    private Long id;

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotBlank(message = "来源单位不能为空")
    @Size(max = 20, message = "来源单位长度不能超过20")
    private String fromUnit;

    @NotBlank(message = "目标单位不能为空")
    @Size(max = 20, message = "目标单位长度不能超过20")
    private String toUnit;

    @NotNull(message = "换算率不能为空")
    private BigDecimal convertRate;
}
