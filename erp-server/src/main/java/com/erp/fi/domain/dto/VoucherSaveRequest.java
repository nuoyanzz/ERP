package com.erp.fi.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class VoucherSaveRequest {

    private Long id;

    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;

    @Size(max = 10, message = "凭证字长度不能超过10")
    private String word;

    private Integer attachCount;

    @Size(max = 500, message = "摘要长度不能超过500")
    private String summary;

    @NotEmpty(message = "凭证明细不能为空")
    private List<VoucherEntryRequest> entries;
}








