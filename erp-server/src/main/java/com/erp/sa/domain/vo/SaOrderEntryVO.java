package com.erp.sa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaOrderEntryVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal deliveredQty;
    private String remark;
}








