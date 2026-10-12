package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_supplier")
public class MdSupplier extends BaseEntity {

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
}








