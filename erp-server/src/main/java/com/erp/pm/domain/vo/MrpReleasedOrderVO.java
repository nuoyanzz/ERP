package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 投放生成的单据摘要。
 */
@Data
public class MrpReleasedOrderVO {

    /** 物料 ID */
    private Long materialId;

    private String materialCode;
    private String materialName;

    /** 生成的单据号 */
    private String orderNo;

    /** 生成的单据 ID */
    private Long orderId;

    /** 单据类型：1 采购订单，2 生产订单 */
    private Integer orderType;

    private BigDecimal qty;
}








