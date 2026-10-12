package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class IncomeStatementVO {

    private String period;
    private List<IncomeItemVO> items;
    private BigDecimal operatingProfit;
    private BigDecimal totalProfit;
    private BigDecimal netProfit;
    private BigDecimal ytdOperatingProfit;
    private BigDecimal ytdTotalProfit;
    private BigDecimal ytdNetProfit;
}








