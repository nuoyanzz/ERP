package com.erp.ar.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 坏账计提单分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArBadDebtQueryRequest extends PageQuery {

    private Long customerId;
    /** 计提期间，格式 YYYY-MM */
    private String period;
    private Integer status;
    /** 单据号模糊查询 */
    private String recordNo;
}













