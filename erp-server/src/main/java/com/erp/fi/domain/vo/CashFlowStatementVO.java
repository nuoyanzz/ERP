package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 现金流量表。
 */
@Data
public class CashFlowStatementVO {

    private String period;
    /** 经营活动 */
    private CashFlowSectionVO operating;
    /** 投资活动 */
    private CashFlowSectionVO investing;
    /** 筹资活动 */
    private CashFlowSectionVO financing;

    /** 经营活动现金流入小计 */
    private BigDecimal operatingInflow;
    /** 经营活动现金流出小计 */
    private BigDecimal operatingOutflow;
    /** 经营活动现金流量净额 */
    private BigDecimal operatingNet;

    /** 投资活动现金流入小计 */
    private BigDecimal investingInflow;
    /** 投资活动现金流出小计 */
    private BigDecimal investingOutflow;
    /** 投资活动现金流量净额 */
    private BigDecimal investingNet;

    /** 筹资活动现金流入小计 */
    private BigDecimal financingInflow;
    /** 筹资活动现金流出小计 */
    private BigDecimal financingOutflow;
    /** 筹资活动现金流量净额 */
    private BigDecimal financingNet;

    /** 现金及现金等价物净增加额 */
    private BigDecimal netIncrease;
    /** 期初现金及现金等价物余额 */
    private BigDecimal openingBalance;
    /** 期末现金及现金等价物余额 */
    private BigDecimal closingBalance;

    /** 各活动分项明细 */
    private List<CashFlowItemVO> items;
}








