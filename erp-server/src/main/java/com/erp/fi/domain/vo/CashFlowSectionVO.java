package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现金流量表的一个活动分类（经营/投资/筹资）。
 */
@Data
public class CashFlowSectionVO {

    /** 活动名称 */
    private String title;
    /** 现金流入小计 */
    private BigDecimal inflow;
    /** 现金流出小计 */
    private BigDecimal outflow;
    /** 现金净流量 */
    private BigDecimal net;
}








