package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_material")
public class MdMaterial extends BaseEntity {

    private String materialCode;
    private String materialName;
    private String spec;
    private Long categoryId;
    private String unit;
    private Integer trackBatch;
    private Integer trackSn;
    private BigDecimal costPrice;
    private BigDecimal safeStock;
    private Integer status;
    private String remark;
}








