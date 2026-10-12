package com.erp.ap.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ApPayableVO {

    private Long id;
    private String payableNo;
    private Long supplierId;
    private String supplierName;
    private String sourceType;
    private String sourceNo;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private Integer status;
    private LocalDate dueDate;
    private String remark;
    private LocalDateTime createTime;
}








