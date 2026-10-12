package com.erp.ic.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class IcCheckListVO {

    private Long id;
    private String checkNo;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate checkDate;
    private Integer status;
    private BigDecimal gainQty;
    private BigDecimal lossQty;
    private LocalDateTime createTime;
}








