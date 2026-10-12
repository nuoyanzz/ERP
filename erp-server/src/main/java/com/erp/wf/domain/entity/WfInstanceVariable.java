package com.erp.wf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 流程实例变量。
 * <p>
 * 流程发起时写入的单据关键字段（金额、部门、发起人等）需要在后续节点求值条件时复用，
 * 因此必须随实例持久化，而不是只在发起请求的内存里存在。
 */
@Data
@TableName("wf_instance_variable")
public class WfInstanceVariable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private String varKey;
    private String varValue;
}








