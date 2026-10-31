package com.erp.ar.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应收账款坏账计提单。
 * <p>
 * 一条记录代表某客户在某会计期间的一次坏账计提，审核时生成会计凭证
 * （借：信用减值损失 / 贷：坏账准备），并把计提金额分摊到该客户的未结清应收单上。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ar_bad_debt")
public class ArBadDebt extends BaseEntity {

    /** 草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 已审核 */
    public static final int STATUS_AUDITED = 1;

    private String recordNo;
    /** 计提所属会计期间，格式 YYYY-MM */
    private String period;
    private Long customerId;
    private BigDecimal amount;
    private String reason;
    private Integer status;
    /** 审核生成的凭证 ID */
    private Long voucherId;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
}













