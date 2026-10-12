package com.erp.ar.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ar_receivable")
public class ArReceivable extends BaseEntity {

    public static final int STATUS_OPEN = 0;
    public static final int STATUS_CLEARED = 1;

    private String receivableNo;
    private Long customerId;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private Integer status;
    private LocalDate dueDate;
    private String remark;
}








