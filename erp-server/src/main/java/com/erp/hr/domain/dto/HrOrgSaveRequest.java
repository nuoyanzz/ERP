package com.erp.hr.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HrOrgSaveRequest {

    private Long id;

    @NotBlank(message = "组织编码不能为空")
    @Size(max = 50, message = "组织编码长度不能超过50")
    private String orgCode;

    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称长度不能超过100")
    private String orgName;

    private Integer orgType;
    private Long parentId;
    private String remark;
}








