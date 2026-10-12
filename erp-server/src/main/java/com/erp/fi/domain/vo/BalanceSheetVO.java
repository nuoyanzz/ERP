package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BalanceSheetVO {

    private String period;
    private List<BalanceSectionVO> sections;
    private BigDecimal assetTotal;
    private BigDecimal liabilityTotal;
    private BigDecimal equityTotal;
    private boolean balanced;
}








