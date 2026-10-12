package com.erp.ic.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class IcTransactionVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long warehouseId;
    private String warehouseName;
    private String batchNo;
    private String tranType;
    private Integer direction;
    private BigDecimal qty;
    private Long bizId;
    private String bizNo;
    private LocalDate bizDate;
    private String remark;
    private LocalDateTime createTime;
}








