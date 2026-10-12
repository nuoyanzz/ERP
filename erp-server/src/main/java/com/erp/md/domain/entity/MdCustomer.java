package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_customer")
public class MdCustomer extends BaseEntity {

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
}








