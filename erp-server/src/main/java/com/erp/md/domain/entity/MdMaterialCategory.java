package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_material_category")
public class MdMaterialCategory extends BaseEntity {

    private String categoryCode;
    private String categoryName;
    private Long parentId;
    private Integer sortNo;
    private Integer status;
    private String remark;
}








