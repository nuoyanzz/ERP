package com.erp.wf.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class WfInstanceDetailVO extends WfInstanceVO {

    /** 流程任务列表 */
    private List<WfTaskVO> tasks;

    /** 审批动作流水 */
    private List<WfTaskRecordVO> records;

    /** 条件分支树 */
    private List<WfNodeBranchVO> branchTree;
}








