package com.erp.pa.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PaInstockSaveRequest {

    private Long id;

    private Long orderId;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "入库日期不能为空")
    private LocalDate instockDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "入库明细不能为空")
    private List<PaInstockEntryRequest> entries;
}








