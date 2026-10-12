package com.erp.md.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MaterialCategorySaveRequest {

    private Long id;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 50, message = "分类编码长度不能超过50")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    private String categoryName;

    @NotNull(message = "上级分类不能为空")
    private Long parentId;

    private Integer sortNo;
    private Integer status;
    private String remark;
}








