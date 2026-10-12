package com.erp.wf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批动作流水，记录每一次审批/驳回/转办/加签/退回等操作。
 * <p>
 * 任务本身只保留最新状态，历史轨迹放在本表，便于前端展示审批记录。
 */
@Data
@TableName("wf_task_record")
public class WfTaskRecord {

    /** 提交 */
    public static final String ACTION_SUBMIT = "SUBMIT";
    /** 同意 */
    public static final String ACTION_APPROVE = "APPROVE";
    /** 驳回 */
    public static final String ACTION_REJECT = "REJECT";
    /** 退回上一节点 */
    public static final String ACTION_ROLLBACK = "ROLLBACK";
    /** 转办 */
    public static final String ACTION_TRANSFER = "TRANSFER";
    /** 加签 */
    public static final String ACTION_COUNTERSIGN = "COUNTERSIGN";
    /** 催办 */
    public static final String ACTION_URGE = "URGE";
    /** 撤回 */
    public static final String ACTION_WITHDRAW = "WITHDRAW";
    /** 抄送 */
    public static final String ACTION_CC = "CC";
    /** 超时提醒 */
    public static final String ACTION_REMIND = "REMIND";
    /** 流程结束 */
    public static final String ACTION_FINISH = "FINISH";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private Long instanceId;
    private Long nodeId;
    private String action;
    private String comment;
    private Long operatorId;
    private LocalDateTime operateTime;
}








