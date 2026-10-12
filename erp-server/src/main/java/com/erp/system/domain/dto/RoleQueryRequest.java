package com.erp.system.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryRequest extends PageQuery {

    private String roleCode;
    private String roleName;
    private Integer status;
}








