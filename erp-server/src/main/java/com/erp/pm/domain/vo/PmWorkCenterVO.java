package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmWorkCenterVO {

    private Long id;
    private String centerCode;
    private String centerName;
    private BigDecimal capacityHours;
    private Integer status;
    private String remark;
}








