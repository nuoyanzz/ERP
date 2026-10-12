package com.erp.sa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SaOutstockListVO {

    private Long id;
    private String outstockNo;
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate outstockDate;
    private Integer status;
    private Integer voucherStatus;
    private BigDecimal totalCost;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}








