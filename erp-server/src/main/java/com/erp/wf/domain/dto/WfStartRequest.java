package com.erp.wf.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WfStartRequest {

    @NotBlank(message = "流程编码不能为空")
    private String definitionKey;

    @Size(max = 30, message = "业务类型长度不能超过30")
    private String bizType;

    private Long bizId;

    @NotBlank(message = "流程标题不能为空")
    @Size(max = 200, message = "流程标题长度不能超过200")
    private String title;

    private String remark;
}








