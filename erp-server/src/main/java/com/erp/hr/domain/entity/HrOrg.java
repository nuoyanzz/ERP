package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_org")
public class HrOrg extends BaseEntity {

    private String orgCode;
    private String orgName;
    private Integer orgType;
    private Long parentId;
    private String remark;
}








