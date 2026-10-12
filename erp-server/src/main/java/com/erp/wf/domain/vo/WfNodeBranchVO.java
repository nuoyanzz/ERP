package com.erp.wf.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件分支树节点。
 * <p>
 * 流程节点是线性的，但条件分支在配置上呈现为「条件节点 + 若干分支」的树形结构，
 * 前端需要这个结构来渲染分支图，因此单独提供 VO 而不复用扁平的节点列表。
 */
@Data
public class WfNodeBranchVO {

    /** 节点 ID */
    private Long id;
    /** 节点编码 */
    private String nodeKey;
    /** 节点名称 */
    private String nodeName;
    /** 节点类型 */
    private Integer nodeType;
    /** 排序号 */
    private Integer sortNo;
    /** 条件表达式 */
    private String conditionExpr;
    /** 审批人类型 */
    private Integer approverType;
    /** 审批人配置 */
    private String approverValue;
    /** 会签模式：1=任一通过，2=全部通过 */
    private Integer multiApprove;
    /** 子分支 */
    private List<WfNodeBranchVO> children = new ArrayList<>();
}








