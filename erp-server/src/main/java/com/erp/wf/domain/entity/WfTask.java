package com.erp.wf.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_task")
public class WfTask extends BaseEntity {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_CANCELLED = 3;
    /** 已转办：原任务不再处理，后续任务由 sourceTaskId 串联 */
    public static final int STATUS_TRANSFERRED = 4;
    /** 已加签：任务转由他人处理 */
    public static final int STATUS_COUNTERSIGNED = 5;
    /** 等待中：前加签场景下原任务被挂起，等加签人通过后恢复 */
    public static final int STATUS_WAITING = 6;

    /** 任务产生动作：CREATE / TRANSFER / COUNTERSIGN / ROLLBACK */
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_TRANSFER = "TRANSFER";
    public static final String ACTION_COUNTERSIGN = "COUNTERSIGN";
    public static final String ACTION_ROLLBACK = "ROLLBACK";

    private Long instanceId;
    private Long nodeId;
    private String nodeKey;
    private Long assigneeId;
    private Integer status;
    private String comment;
    private Long operateBy;
    private LocalDateTime operateTime;
    /** 转办/加签来源任务 */
    private Long sourceTaskId;
    /** 任务产生动作 */
    private String actionType;
    /** 超时时间，节点配置 timeout_hours 时写入 */
    private LocalDateTime timeoutAt;
    /** 已催办次数 */
    private Integer remindCount;
}








