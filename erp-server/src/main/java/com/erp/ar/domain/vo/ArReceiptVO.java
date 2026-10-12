package com.erp.ar.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArReceiptVO {

    private Long id;
    private String receiptNo;
    private Long customerId;
    private String customerName;
    private LocalDate receiptDate;
    private BigDecimal amount;
    private Integer status;
    private Long voucherId;
    private String remark;
    private LocalDateTime createTime;
    private List<ArVerifyVO> verifies;
}








