package com.erp.ap.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ApPaymentSaveRequest {

    private Long id;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "付款日期不能为空")
    private LocalDate paymentDate;

    @NotNull(message = "付款金额不能为空")
    @DecimalMin(value = "0.0001", message = "付款金额必须大于0")
    private BigDecimal amount;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








