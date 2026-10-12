package com.erp.wf.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WfTaskVO {

    private Long id;
    private Long instanceId;
    private String instanceTitle;
    private String definitionName;
    private String bizType;
    private Long bizId;
    private String nodeKey;
    private String nodeName;
    private Long assigneeId;
    private String assigneeName;
    private Integer status;
    private String comment;
    private Long operateBy;
    private String operateByName;
    private LocalDateTime operateTime;
    private LocalDateTime createTime;
    /** 转办/加签来源任务 */
    private Long sourceTaskId;
    /** 任务产生动作 */
    private String actionType;
    /** 超时时间 */
    private LocalDateTime timeoutAt;
    /** 已催办次数 */
    private Integer remindCount;
}








