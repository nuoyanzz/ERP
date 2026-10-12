package com.erp.md.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierVO {

    private Long id;
    private String supplierCode;
    private String supplierName;
    private String taxNo;
    private String settleType;
    private Integer level;
    private String contactName;
    private String contactPhone;
    private String address;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}








