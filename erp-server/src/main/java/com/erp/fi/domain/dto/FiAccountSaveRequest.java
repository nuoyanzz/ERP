package com.erp.fi.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FiAccountSaveRequest {

    private Long id;

    @NotBlank(message = "科目编码不能为空")
    @Size(max = 20, message = "科目编码长度不能超过20")
    private String subjectCode;

    @NotBlank(message = "科目名称不能为空")
    @Size(max = 100, message = "科目名称长度不能超过100")
    private String subjectName;

    @NotNull(message = "上级科目不能为空")
    private Long parentId;

    @Min(value = 1, message = "科目类别取值1-5")
    @Max(value = 5, message = "科目类别取值1-5")
    private Integer category;

    @Min(value = 1, message = "余额方向取值1-2")
    @Max(value = 2, message = "余额方向取值1-2")
    private Integer balanceDir;

    private Integer assistCustomer;
    private Integer assistSupplier;
    private Integer assistDept;
    private Integer assistProject;

    @Size(max = 10, message = "核算币别长度不能超过10")
    private String currency;

    private Integer status;
    private String remark;
}








