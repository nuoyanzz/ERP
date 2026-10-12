package com.erp.wf.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WfDefinitionSaveRequest {

    private Long id;

    @NotBlank(message = "流程编码不能为空")
    @Size(max = 50, message = "流程编码长度不能超过50")
    private String definitionKey;

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 100, message = "流程名称长度不能超过100")
    private String definitionName;

    @Size(max = 30, message = "业务类型长度不能超过30")
    private String bizType;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;

    @NotEmpty(message = "流程节点不能为空")
    private List<WfNodeRequest> nodes;

    private String remark;
}








