package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BalanceSectionVO {

    private String title;
    private List<BalanceLineVO> lines;
    private BigDecimal subtotal;
}








