package com.erp.pa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaOrderListVO {

    private Long id;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private Integer status;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}








