package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IncomeItemVO {

    private String name;
    private BigDecimal currentAmount;
    private BigDecimal ytdAmount;
}








