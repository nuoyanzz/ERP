package com.erp.ic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ic_check")
public class IcCheck extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    private String checkNo;
    private Long warehouseId;
    private LocalDate checkDate;
    private Integer status;
    private BigDecimal gainQty;
    private BigDecimal lossQty;
    /** 盘盈金额 */
    private BigDecimal gainAmount;
    /** 盘亏金额 */
    private BigDecimal lossAmount;
    /** 差异原因码 */
    private String reasonCode;
    private Long voucherId;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
}








