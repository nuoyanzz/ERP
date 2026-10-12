package com.erp.ic.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IcCheckEntryVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private BigDecimal bookQty;
    private BigDecimal countQty;
    private BigDecimal gainQty;
    private BigDecimal lossQty;
    private String remark;
}








