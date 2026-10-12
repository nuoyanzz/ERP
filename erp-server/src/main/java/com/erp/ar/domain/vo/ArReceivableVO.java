package com.erp.ar.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ArReceivableVO {

    private Long id;
    private String receivableNo;
    private Long customerId;
    private String customerName;
    private String sourceType;
    private String sourceNo;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private BigDecimal unreceivedAmount;
    private Integer status;
    private LocalDate dueDate;
    private String remark;
    private LocalDateTime createTime;
}








