package com.erp.sa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SaReturnListVO {

    private Long id;
    private String returnNo;
    private Long sourceOutstockId;
    private String sourceOutstockNo;
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate returnDate;
    private Integer status;
    private Integer voucherStatus;
    private BigDecimal totalCost;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}








