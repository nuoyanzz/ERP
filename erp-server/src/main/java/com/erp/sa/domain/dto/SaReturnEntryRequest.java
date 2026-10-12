package com.erp.sa.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaReturnEntryRequest {

    @NotNull(message = "来源出库明细不能为空")
    private Long sourceEntryId;

    @NotNull(message = "退货数量不能为空")
    @DecimalMin(value = "0.0001", message = "退货数量必须大于0")
    private BigDecimal qty;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








