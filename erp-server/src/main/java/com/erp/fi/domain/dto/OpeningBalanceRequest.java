package com.erp.fi.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 期初余额录入请求。
 */
@Data
public class OpeningBalanceRequest {

    /** 科目 ID */
    @NotNull(message = "会计科目不能为空")
    private Long accountId;

    /** 期初所属期间，格式 YYYY-MM */
    @NotNull(message = "会计期间不能为空")
    private String period;

    private String currency;

    /** 借方余额 */
    private BigDecimal debitAmount;

    /** 贷方余额 */
    private BigDecimal creditAmount;

    /** 辅助核算类型：CUSTOMER / SUPPLIER / DEPT / PROJECT */
    private String auxType;

    /** 辅助核算对象 ID */
    private Long auxId;
}








