package com.erp.pa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pa_order")
public class PaOrder extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_CLOSED = 3;

    private String orderNo;
    private Long supplierId;
    private Long deptId;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private Integer status;
    private BigDecimal totalAmount;
    private Long auditBy;
    private java.time.LocalDateTime auditTime;
    private String closeReason;
    private String remark;
}








