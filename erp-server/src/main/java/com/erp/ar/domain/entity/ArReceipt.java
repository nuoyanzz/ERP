package com.erp.ar.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ar_receipt")
public class ArReceipt extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    private String receiptNo;
    private Long customerId;
    private LocalDate receiptDate;
    private BigDecimal amount;
    private Integer status;
    private Long voucherId;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
}








