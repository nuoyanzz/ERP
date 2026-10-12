package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherEntryVO {

    private Long id;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private Integer direction;
    private BigDecimal amount;
    private String summary;
    private String assist;
    private Integer sortNo;
}








