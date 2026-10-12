package com.erp.ar.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ArReceiptSaveRequest {

    private Long id;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "收款日期不能为空")
    private LocalDate receiptDate;

    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0.0001", message = "收款金额必须大于0")
    private BigDecimal amount;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








