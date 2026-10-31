package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 汇率展示对象。
 */
@Data
public class ExchangeRateVO {

    private Long id;
    private String currency;
    private String period;
    private BigDecimal rate;
    private Integer rateType;
    private String createTime;
}
