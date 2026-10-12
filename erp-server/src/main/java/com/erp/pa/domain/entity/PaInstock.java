package com.erp.pa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pa_instock")
public class PaInstock extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    public static final int VOUCHER_STATUS_NONE = 0;
    public static final int VOUCHER_STATUS_DONE = 1;
    public static final int VOUCHER_STATUS_FAILED = 2;

    private String instockNo;
    private Long orderId;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate instockDate;
    private Integer status;
    private Integer voucherStatus;
    private Long voucherId;
    private BigDecimal totalAmount;
    /** 不含税金额 */
    private BigDecimal totalCost;
    /** 进项税额 */
    private BigDecimal taxAmount;
    private Long auditBy;
    private java.time.LocalDateTime auditTime;
    private String remark;
}








