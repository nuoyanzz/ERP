package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_biz_account_rule")
public class FiBizAccountRule extends BaseEntity {

    private String bizType;
    private String ruleKey;
    private String subjectCode;
    private String remark;
}








