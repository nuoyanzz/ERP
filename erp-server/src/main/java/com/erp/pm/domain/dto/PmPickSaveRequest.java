package com.erp.pm.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PmPickSaveRequest {

    private Long id;

    @NotNull(message = "生产订单不能为空")
    private Long moId;

    @NotNull(message = "领料仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "领料日期不能为空")
    private LocalDate pickDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "领料明细不能为空")
    private List<PmPickEntryRequest> entries;
}








