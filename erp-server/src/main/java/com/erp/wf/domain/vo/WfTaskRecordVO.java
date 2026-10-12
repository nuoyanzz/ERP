package com.erp.wf.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批动作流水。
 */
@Data
public class WfTaskRecordVO {

    private Long id;
    private Long taskId;
    private Long instanceId;
    private Long nodeId;
    private String nodeName;
    /** 动作：APPROVE / REJECT / ROLLBACK / TRANSFER / COUNTERSIGN / URGE / WITHDRAW / CC / REMIND */
    private String action;
    private String comment;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operateTime;
}








