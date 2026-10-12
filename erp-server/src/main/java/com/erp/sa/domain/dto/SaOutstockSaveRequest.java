package com.erp.sa.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaOutstockSaveRequest {

    private Long id;

    private Long orderId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "出库日期不能为空")
    private LocalDate outstockDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "出库明细不能为空")
    private List<SaOutstockEntryRequest> entries;
}








