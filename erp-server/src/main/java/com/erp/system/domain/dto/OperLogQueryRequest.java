package com.erp.system.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQueryRequest extends PageQuery {

    private String module;
    private String operType;
    private String operUsername;
    private Integer success;
}
