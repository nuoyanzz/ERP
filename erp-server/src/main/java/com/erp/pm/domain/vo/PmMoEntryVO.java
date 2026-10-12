package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmMoEntryVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal requireQty;
    private BigDecimal pickedQty;
    private String remark;
}








