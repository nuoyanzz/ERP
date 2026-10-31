package com.erp.md.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MdUnitSaveRequest {

    private Long id;

    @NotBlank(message = "计量单位编码不能为空")
    @Size(max = 30, message = "计量单位编码长度不能超过30")
    private String unitCode;

    @NotBlank(message = "计量单位名称不能为空")
    @Size(max = 50, message = "计量单位名称长度不能超过50")
    private String unitName;

    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
