package com.erp.pa.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaInstockEntryRequest {

    private Long orderEntryId;

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

    @Size(max = 50, message = "批次号长度不能超过50")
    private String batchNo;

    private String remark;
}








