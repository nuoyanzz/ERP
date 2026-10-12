package com.erp.ap.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApPaymentVO {

    private Long id;
    private String paymentNo;
    private Long supplierId;
    private String supplierName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private Integer status;
    private Long voucherId;
    private String remark;
    private LocalDateTime createTime;
    private List<ApVerifyVO> verifies;
}








