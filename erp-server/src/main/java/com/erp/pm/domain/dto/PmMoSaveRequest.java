package com.erp.pm.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PmMoSaveRequest {

    private Long id;

    @NotNull(message = "生产物料不能为空")
    private Long materialId;

    @NotNull(message = "BOM 不能为空")
    private Long bomId;

    @NotNull(message = "生产数量不能为空")
    @DecimalMin(value = "0.0001", message = "生产数量必须大于0")
    private BigDecimal qty;

    @NotNull(message = "成品仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "计划开工日期不能为空")
    private LocalDate planStartDate;

    private LocalDate planEndDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








