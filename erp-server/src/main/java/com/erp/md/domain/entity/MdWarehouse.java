package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_warehouse")
public class MdWarehouse extends BaseEntity {

    private String warehouseCode;
    private String warehouseName;
    private Integer whType;
    private String location;
    private Integer status;
    private String remark;
}








