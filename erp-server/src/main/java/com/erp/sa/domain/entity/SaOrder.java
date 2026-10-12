package com.erp.sa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sa_order")
public class SaOrder extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_CLOSED = 3;

    private String orderNo;
    private Long customerId;
    private Long deptId;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private Integer status;
    private BigDecimal totalAmount;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String closeReason;
    private String remark;
}








