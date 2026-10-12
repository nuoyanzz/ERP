package com.erp.ap.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ap_payable")
public class ApPayable extends BaseEntity {

    public static final int STATUS_OPEN = 0;
    public static final int STATUS_CLEARED = 1;

    private String payableNo;
    private Long supplierId;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private Integer status;
    private LocalDate dueDate;
    private String remark;
}








