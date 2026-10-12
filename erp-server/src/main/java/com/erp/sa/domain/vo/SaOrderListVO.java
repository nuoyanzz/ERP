package com.erp.sa.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SaOrderListVO {

    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private LocalDate orderDate;
    private Integer status;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}








