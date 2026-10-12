package com.erp.pa.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PaReturnSaveRequest {

    private Long id;

    @NotNull(message = "来源入库单不能为空")
    private Long sourceInstockId;

    @NotNull(message = "退货日期不能为空")
    private LocalDate returnDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "退货明细不能为空")
    private List<PaReturnEntryRequest> entries;
}








