package com.erp.sa.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaOrderEntryRequest {

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.0001", message = "数量必须大于0")
    private BigDecimal qty;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0", message = "单价不能小于0")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "税率不能小于0")
    private BigDecimal taxRate;

    private String remark;
}








