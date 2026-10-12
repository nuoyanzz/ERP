package com.erp.wf.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfInstanceQueryRequest extends PageQuery {

    private String definitionKey;
    private String bizType;
    private Integer status;
    private String title;
}








