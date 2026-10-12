package com.erp.md.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MaterialSaveRequest {

    private Long id;

    @NotBlank(message = "物料编码不能为空")
    @Size(max = 50, message = "物料编码长度不能超过50")
    private String materialCode;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 100, message = "物料名称长度不能超过100")
    private String materialName;

    @Size(max = 200, message = "规格型号长度不能超过200")
    private String spec;

    private Long categoryId;

    @Size(max = 20, message = "计量单位长度不能超过20")
    private String unit;

    private Integer trackBatch;
    private Integer trackSn;
    private Integer status;
    private String remark;
}








