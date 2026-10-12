package com.erp.pa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaReturnEntryVO {

    private Long id;
    private Long sourceEntryId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private String batchNo;
    private String remark;
}








