package com.erp.ic.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IcTransferEntryVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private BigDecimal qty;
    private String remark;
}








