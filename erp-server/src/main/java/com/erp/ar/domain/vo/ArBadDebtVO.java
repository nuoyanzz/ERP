package com.erp.ar.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 坏账计提单返回对象。
 */
@Data
public class ArBadDebtVO {

    private Long id;
    private String recordNo;
    private String period;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    private String reason;
    private Integer status;
    private Long voucherId;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
    private LocalDateTime createTime;
}













