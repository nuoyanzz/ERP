package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmRoutingOperationVO {

    private Long id;
    private Long routingId;
    private String operationNo;
    private String operationName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private BigDecimal standardHours;
    private BigDecimal standardQty;
    private BigDecimal laborRate;
    private BigDecimal overheadRate;
    private Integer sortNo;
    private String remark;
}








