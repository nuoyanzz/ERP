package com.erp.wf.domain.vo;

import lombok.Data;

@Data
public class WfNodeVO {

    private Long id;
    private Long definitionId;
    private String nodeKey;
    private String nodeName;
    private Integer nodeType;
    private Integer sortNo;
    private Integer approverType;
    private String approverValue;
    private Integer multiApprove;
    /** 条件表达式 */
    private String conditionExpr;
    /** 超时小时数 */
    private Integer timeoutHours;
    /** 兜底审批人 */
    private String defaultAssignee;
    /** 是否抄送 */
    private Integer ccFlag;
    private String remark;
}








