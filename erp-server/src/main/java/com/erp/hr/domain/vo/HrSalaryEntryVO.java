package com.erp.hr.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HrSalaryEntryVO {

    private Long id;
    private Integer itemType;
    private String itemName;
    private BigDecimal amount;
}








