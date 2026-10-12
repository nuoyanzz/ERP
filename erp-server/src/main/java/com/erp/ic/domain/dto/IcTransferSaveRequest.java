package com.erp.ic.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class IcTransferSaveRequest {

    private Long id;

    @NotNull(message = "调出仓库不能为空")
    private Long fromWarehouseId;

    @NotNull(message = "调入仓库不能为空")
    private Long toWarehouseId;

    @NotNull(message = "调拨日期不能为空")
    private LocalDate transferDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "调拨明细不能为空")
    private List<IcTransferEntryRequest> entries;
}








