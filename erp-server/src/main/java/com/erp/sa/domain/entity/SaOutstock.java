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
@TableName("sa_outstock")
public class SaOutstock extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    public static final int VOUCHER_STATUS_NONE = 0;
    public static final int VOUCHER_STATUS_DONE = 1;
    public static final int VOUCHER_STATUS_FAILED = 2;

    private String outstockNo;
    private Long orderId;
    private Long customerId;
    private Long warehouseId;
    private LocalDate outstockDate;
    private Integer status;
    private Integer voucherStatus;
    private Long voucherId;
    private BigDecimal totalCost;
    private BigDecimal totalAmount;
    /** 销项税额 */
    private BigDecimal taxAmount;
    /** 不含税金额 */
    private BigDecimal netAmount;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;

    /** 所属部门，数据权限按此字段过滤 */
    private Long deptId;
}








