package com.erp.wf.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WfInstanceVO {

    private Long id;
    private Long definitionId;
    private String definitionKey;
    private String definitionName;
    private String bizType;
    private Long bizId;
    private String title;
    private Long starterId;
    private String starterName;
    private Long currentNodeId;
    private String currentNodeName;
    private Integer status;
    /** 挂起原因 */
    private String suspendReason;
    /** 退回次数 */
    private Integer rollbackCount;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}








