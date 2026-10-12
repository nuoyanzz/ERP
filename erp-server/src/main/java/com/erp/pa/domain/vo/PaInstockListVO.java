package com.erp.pa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaInstockListVO {

    private Long id;
    private String instockNo;
    private Long orderId;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate instockDate;
    private Integer status;
    private Integer voucherStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}








