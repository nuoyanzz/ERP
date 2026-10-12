package com.erp.md.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseSaveRequest {

    private Long id;

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 50, message = "仓库编码长度不能超过50")
    private String warehouseCode;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称长度不能超过100")
    private String warehouseName;

    @Min(value = 1, message = "仓库类型：1实仓 2虚仓 3待检仓")
    @Max(value = 3, message = "仓库类型：1实仓 2虚仓 3待检仓")
    private Integer whType;

    @Size(max = 200, message = "地点长度不能超过200")
    private String location;

    private Integer status;
    private String remark;
}








