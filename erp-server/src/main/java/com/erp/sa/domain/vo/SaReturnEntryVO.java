package com.erp.sa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaReturnEntryVO {

    private Long id;
    private Long sourceEntryId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private String batchNo;
    private String remark;
}








