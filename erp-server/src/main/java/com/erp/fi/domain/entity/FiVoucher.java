package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_voucher")
public class FiVoucher extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_SUBMITTED = 1;
    public static final int STATUS_AUDITED = 2;
    public static final int STATUS_POSTED = 3;
    public static final int STATUS_RED_FLUSHED = 4;
    /** 已作废 */
    public static final int STATUS_VOID = 5;

    private String voucherNo;
    private String word;
    private LocalDate voucherDate;
    private String period;
    private Integer attachCount;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private Integer status;
    private Integer redFlush;
    private Long sourceVoucherId;
    private String summary;
    private Long submitBy;
    private LocalDateTime submitTime;
    private Long auditBy;
    private LocalDateTime auditTime;
    private Long postBy;
    private LocalDateTime postTime;

    /** 作废原因 */
    private String voidReason;
    private Long voidBy;
    private LocalDateTime voidTime;
    /** 反记账留痕 */
    private Long unpostBy;
    private LocalDateTime unpostTime;
    /** 来源业务类型，如 SA_OUTSTOCK */
    private String sourceType;
    private Long sourceId;
}








