package com.erp.ic.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IcStockVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal lockedQty;
    private BigDecimal availableQty;
    private LocalDateTime updateTime;
}








