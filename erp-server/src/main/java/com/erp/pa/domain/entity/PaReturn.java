package com.erp.pa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pa_return")
public class PaReturn extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    public static final int VOUCHER_STATUS_NONE = 0;
    public static final int VOUCHER_STATUS_DONE = 1;
    public static final int VOUCHER_STATUS_FAILED = 2;

    private String returnNo;
    private Long sourceInstockId;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate returnDate;
    private Integer status;
    private Integer voucherStatus;
    private Long voucherId;
    private BigDecimal totalAmount;
    /** 不含税金额 */
    private BigDecimal totalCost;
    /** 进项税额 */
    private BigDecimal taxAmount;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
}








