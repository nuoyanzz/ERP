package com.erp.wf.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WfDefinitionVO {

    private Long id;
    private String definitionKey;
    private String definitionName;
    private String bizType;
    private String description;
    private Integer status;
    private Integer version;
    private LocalDateTime createTime;
    /** 节点列表 */
    private List<WfNodeVO> nodes;
    /** 条件分支树 */
    private List<WfNodeBranchVO> branchTree;
}








