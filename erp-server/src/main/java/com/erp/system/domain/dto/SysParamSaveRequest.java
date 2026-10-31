package com.erp.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统参数保存请求。
 */
@Data
public class SysParamSaveRequest {

    private Long id;

    @NotBlank(message = "参数键不能为空")
    @Size(max = 100, message = "参数键长度不能超过100")
    private String paramKey;

    @Size(max = 500, message = "参数值长度不能超过500")
    private String paramValue;

    @Size(max = 100, message = "参数名称长度不能超过100")
    private String paramName;

    /** 参数类型：STRING / NUMBER / BOOLEAN / JSON */
    @NotBlank(message = "参数类型不能为空")
    @Pattern(regexp = "^(STRING|NUMBER|BOOLEAN|JSON)$", message = "参数类型只能是 STRING / NUMBER / BOOLEAN / JSON")
    private String paramType;

    /** 状态：0 启用 1 停用 */
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}













