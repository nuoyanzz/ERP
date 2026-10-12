package com.erp.pm.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PmReportWorkSaveRequest {

    private Long id;

    @NotNull(message = "生产订单不能为空")
    private Long moId;

    /** 工序（来自工艺路线） */
    private Long operationId;

    /** 报工人 */
    private Long employeeId;

    @NotNull(message = "报工日期不能为空")
    private LocalDate reportDate;

    @NotNull(message = "报工数量不能为空")
    @DecimalMin(value = "0.0001", message = "报工数量必须大于0")
    private BigDecimal qty;

    /** 合格数量，为空时按报工数量计 */
    @DecimalMin(value = "0", message = "合格数量不能小于0")
    private BigDecimal qualifiedQty;

    /** 报废数量 */
    @DecimalMin(value = "0", message = "报废数量不能小于0")
    private BigDecimal scrapQty;

    @NotNull(message = "工时不能为空")
    @DecimalMin(value = "0", message = "工时不能小于0")
    private BigDecimal workHours;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








