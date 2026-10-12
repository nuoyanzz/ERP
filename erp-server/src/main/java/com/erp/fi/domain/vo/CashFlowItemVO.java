package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现金流量表明细行。
 */
@Data
public class CashFlowItemVO {

    /** 活动类型：1 经营 2 投资 3 筹资 */
    private Integer cashFlowType;
    private String subjectCode;
    private String subjectName;
    /** 本期现金流入 */
    private BigDecimal inflow;
    /** 本期现金流出 */
    private BigDecimal outflow;
    /** 本期净流量 */
    private BigDecimal net;
    /** 年初至本期累计流入 */
    private BigDecimal ytdInflow;
    /** 年初至本期累计流出 */
    private BigDecimal ytdOutflow;
}








