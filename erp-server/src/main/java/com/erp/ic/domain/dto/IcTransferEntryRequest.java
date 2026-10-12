package com.erp.ic.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IcTransferEntryRequest {

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "调拨数量不能为空")
    @DecimalMin(value = "0.0001", message = "调拨数量必须大于0")
    private BigDecimal qty;

    @Size(max = 50, message = "批次号长度不能超过50")
    private String batchNo;

    private String remark;
}








