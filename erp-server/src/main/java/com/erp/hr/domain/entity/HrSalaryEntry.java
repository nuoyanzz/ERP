package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("hr_salary_entry")
public class HrSalaryEntry {

    private Long id;
    private Long salaryId;
    private Integer itemType;
    private String itemName;
    private BigDecimal amount;
}








