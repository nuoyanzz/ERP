package com.erp.system.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysParamQueryRequest extends PageQuery {

    /** 参数键，模糊查询 */
    private String paramKey;
    /** 参数名称，模糊查询 */
    private String paramName;
    /** 参数类型：STRING / NUMBER / BOOLEAN / JSON */
    private String paramType;
    /** 状态：0 启用 1 停用 */
    private Integer status;
}













