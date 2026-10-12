package com.erp.pa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaReturnListVO {

    private Long id;
    private String returnNo;
    private Long sourceInstockId;
    private String sourceInstockNo;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate returnDate;
    private Integer status;
    private Integer voucherStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}








