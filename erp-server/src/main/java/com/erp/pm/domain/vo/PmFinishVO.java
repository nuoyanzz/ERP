package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PmFinishVO {

    private Long id;
    private String finishNo;
    private Long moId;
    private String moNo;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate finishDate;
    private BigDecimal qty;
    private BigDecimal costAmount;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal overheadCost;
    private BigDecimal unitCost;
    private Integer status;
    private Long voucherId;
    private String remark;
    private LocalDateTime createTime;
}








