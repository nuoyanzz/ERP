package com.erp.fi.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 汇率录入请求。
 */
@Data
public class ExchangeRateSaveRequest {

    private Long id;

    @NotBlank(message = "币别不能为空")
    private String currency;

    @NotBlank(message = "期间不能为空")
    private String period;

    @NotNull(message = "汇率不能为空")
    private BigDecimal rate;

    /** 1 期间汇率 2 调整汇率 */
    private Integer rateType;
}
