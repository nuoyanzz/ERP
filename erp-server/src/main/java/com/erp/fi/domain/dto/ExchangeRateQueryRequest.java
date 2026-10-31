package com.erp.fi.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExchangeRateQueryRequest extends PageQuery {

    private String currency;
    private String period;
}
