package com.erp.md.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class MdPriceQueryRequest extends PageQuery {

    private String priceNo;
    private String priceName;
    private Long customerId;
    private Long supplierId;
    private Long materialId;
    private String currency;
    private Integer status;

    /** 按生效日期筛选：取大于等于该日期的记录 */
    private LocalDate effectiveDate;

    /** 按失效日期筛选：取小于等于该日期的记录 */
    private LocalDate expiryDate;
}
