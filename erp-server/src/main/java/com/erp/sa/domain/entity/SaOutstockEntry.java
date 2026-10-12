package com.erp.sa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sa_outstock_entry")
public class SaOutstockEntry {

    private Long id;
    private Long outstockId;
    private Long orderEntryId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private BigDecimal returnedQty;
    private String batchNo;
    private String remark;
}








