package com.erp.ic.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IcCheckEntryRequest {

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "实盘数量不能为空")
    @DecimalMin(value = "0", message = "实盘数量不能小于0")
    private BigDecimal countQty;

    private String batchNo;
    private String remark;
}








