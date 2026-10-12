package com.erp.md.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerVO {

    private Long id;
    private String customerCode;
    private String customerName;
    private BigDecimal creditLimit;
    private String currency;
    private String taxNo;
    private String contactName;
    private String contactPhone;
    private String address;
    private Long salesDeptId;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}








