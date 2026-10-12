package com.erp.fi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherLine {

    private Integer direction;
    private String ruleKey;
    private String subjectCode;
    private BigDecimal amount;
    private String summary;

    /** 结构化辅助核算类型：CUSTOMER / SUPPLIER / DEPT / PROJECT */
    private String auxType;
    /** 辅助核算对象 ID */
    private Long auxId;
    /** 辅助核算展示文本，冗余存储便于查询与导出 */
    private String auxName;
    /** 原币币别 */
    private String currency;
    /** 原币金额；为空时视为与本币金额一致 */
    private BigDecimal originalAmount;
    /** 折算汇率 */
    private BigDecimal exchangeRate;
}








