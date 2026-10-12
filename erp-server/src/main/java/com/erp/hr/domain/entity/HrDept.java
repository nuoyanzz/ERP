package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_dept")
public class HrDept extends BaseEntity {

    private String deptCode;
    private String deptName;
    private Long orgId;
    private Long parentId;
    private String remark;
}








