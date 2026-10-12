package com.erp.pa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaOrderEntryVO {

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
    private BigDecimal receivedQty;
    private String remark;
}








