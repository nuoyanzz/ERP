package com.erp.sa.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaOrderSaveRequest {

    private Long id;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    private Long deptId;

    @NotNull(message = "订单日期不能为空")
    private LocalDate orderDate;

    private LocalDate deliveryDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "订单明细不能为空")
    private List<SaOrderEntryRequest> entries;
}








