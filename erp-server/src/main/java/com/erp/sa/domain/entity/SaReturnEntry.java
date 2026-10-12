package com.erp.sa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sa_return_entry")
public class SaReturnEntry {

    private Long id;
    private Long returnId;
    private Long sourceEntryId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private String batchNo;
    private String remark;
}








