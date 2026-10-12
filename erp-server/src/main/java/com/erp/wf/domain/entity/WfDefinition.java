package com.erp.wf.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_definition")
public class WfDefinition extends BaseEntity {

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    private String definitionKey;
    private String definitionName;
    private String bizType;
    private String description;
    private Integer status;
    private Integer version;
    private String remark;
}








