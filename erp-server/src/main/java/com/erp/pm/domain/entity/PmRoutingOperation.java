package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工艺路线工序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_routing_operation")
public class PmRoutingOperation extends BaseEntity {

    private Long routingId;

    /** 工序号（如 OP10、OP20） */
    private String operationNo;

    /** 工序名称 */
    private String operationName;

    /** 工作中心 */
    private Long workCenterId;

    /** 标准工时（小时） */
    private BigDecimal standardHours;

    /** 标准产能：每 standardHours 小时的标准产出数量 */
    private BigDecimal standardQty;

    /** 人工费率（元/小时） */
    private BigDecimal laborRate;

    /** 制造费用率（元/小时） */
    private BigDecimal overheadRate;

    /** 排序号 */
    private Integer sortNo;

    private String remark;
}








