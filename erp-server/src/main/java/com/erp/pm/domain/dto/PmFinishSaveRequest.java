package com.erp.pm.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PmFinishSaveRequest {

    private Long id;

    @NotNull(message = "生产订单不能为空")
    private Long moId;

    @NotNull(message = "成品仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "完工日期不能为空")
    private LocalDate finishDate;

    @NotNull(message = "完工数量不能为空")
    @DecimalMin(value = "0.0001", message = "完工数量必须大于0")
    private BigDecimal qty;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








