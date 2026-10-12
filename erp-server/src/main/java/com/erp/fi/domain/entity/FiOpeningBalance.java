package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 科目期初余额。用于承接存量账套，是系统启用的第一个期间的余额起点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_opening_balance")
public class FiOpeningBalance extends BaseEntity {

    /** 账套 ID，预留多账套 */
    private Long sobId;
    private Long accountId;
    /** 期初所属期间，格式 YYYY-MM */
    private String period;
    private String currency;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    /** 辅助核算类型 */
    private String auxType;
    /** 辅助核算对象 ID */
    private Long auxId;
}








