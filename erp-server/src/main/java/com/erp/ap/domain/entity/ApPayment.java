package com.erp.ap.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ap_payment")
public class ApPayment extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    private String paymentNo;
    private Long supplierId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private Integer status;
    private Long voucherId;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;

    /** 所属部门，数据权限按此字段过滤 */
    private Long deptId;
}








