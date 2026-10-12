package com.erp.ic.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class IcCheckSaveRequest {

    private Long id;

    @NotNull(message = "盘点仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "盘点日期不能为空")
    private LocalDate checkDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "盘点明细不能为空")
    private List<IcCheckEntryRequest> entries;
}








