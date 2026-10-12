package com.erp.pm.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmPickEntryRequest {

    @NotNull(message = "订单子项不能为空")
    private Long moEntryId;

    @NotNull(message = "领料数量不能为空")
    @DecimalMin(value = "0.0001", message = "领料数量必须大于0")
    private BigDecimal qty;

    /**
     * 实际领用物料。为空表示按 BOM 主料领用；
     * 传入替代料 ID 时表示用替代料顶替主料领用，成本按替代料成本价计算。
     */
    private Long actualMaterialId;

    @Size(max = 50, message = "批次号长度不能超过50")
    private String batchNo;

    private String remark;
}








