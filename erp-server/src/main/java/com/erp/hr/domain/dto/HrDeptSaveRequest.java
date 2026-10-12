package com.erp.hr.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HrDeptSaveRequest {

    private Long id;

    @NotBlank(message = "部门编码不能为空")
    @Size(max = 50, message = "部门编码长度不能超过50")
    private String deptCode;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100, message = "部门名称长度不能超过100")
    private String deptName;

    private Long orgId;

    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    private String remark;
}








