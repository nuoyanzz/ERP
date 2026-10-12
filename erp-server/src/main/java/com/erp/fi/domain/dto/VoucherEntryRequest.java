package com.erp.fi.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherEntryRequest {

    @NotNull(message = "会计科目不能为空")
    private Long accountId;

    @NotNull(message = "借贷方向不能为空")
    private Integer direction;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    private String summary;
    private String assist;

    /** 结构化辅助核算类型：CUSTOMER / SUPPLIER / DEPT / PROJECT */
    private String auxType;
    /** 辅助核算对象 ID */
    private Long auxId;
}








