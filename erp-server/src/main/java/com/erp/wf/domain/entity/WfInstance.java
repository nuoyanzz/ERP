package com.erp.wf.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_instance")
public class WfInstance extends BaseEntity {

    /** 运行中（含挂起） */
    public static final int STATUS_RUNNING = 0;
    /** 审批通过 */
    public static final int STATUS_APPROVED = 1;
    /** 审批驳回 */
    public static final int STATUS_REJECTED = 2;
    /** 发起人撤回/撤销 */
    public static final int STATUS_CANCELLED = 3;
    /** 挂起：解析不到审批人等原因导致流程无法继续 */
    public static final int STATUS_SUSPENDED = 4;

    private Long definitionId;
    private String bizType;
    private Long bizId;
    private String title;
    private Long starterId;
    private Long currentNodeId;
    private Integer status;
    private LocalDateTime finishTime;
    /** 挂起原因 */
    private String suspendReason;
    /** 当前退回次数，用于限制退回次数防止死循环 */
    private Integer rollbackCount;
    private String remark;
}








